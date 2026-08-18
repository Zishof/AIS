package ais.action.master.akunting.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Footer;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Toolbar;

import ais.action.master.akunting.util.CommonAkunting;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.rab.helper.AmbilDataSatuanKerjaBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.Transaksi;
import ais.database.model.rab.SatuanKerja;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TransaksiJurnalPenerimaanHelper extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400318L;
	private MyGrid grid;

	private static long ids = 100000L;

	private boolean edit = false;
	private boolean delete = false;

	private String parentCode;

	private Transaksi transaksiUtama;

	private AmbilDataAkunDebetBanbox kodeAkun;
	private Textbox keteranganAkun;
	private Label noref;
	private MyDatebox tanggal;
	private Doublebox jumlah;

	private GrupTransaksi grupTransaksi;

	private Label labelTotal = new Label("0");

	private EventListener eventListener;
	private MyToolbarbuttonConfig save;

	private List<Transaksi> newTransaksis = new ArrayList<Transaksi>();
	private double total;
	private AmbilDataSatuanKerjaBanbox satuanKerja;

	public TransaksiJurnalPenerimaanHelper(Transaksi transaksiUtama) throws Exception {
		super();
		this.transaksiUtama = transaksiUtama;

		initWindow();
	}

	public TransaksiJurnalPenerimaanHelper(Transaksi transaksiUtama, String title, String border, boolean closable)
			throws Exception {
		super(title, border, closable);
		this.transaksiUtama = transaksiUtama;

		initWindow();
	}

	public void initWindow() throws Exception {
		parentCode = transaksiUtama.getParentCode();
		grupTransaksi = transaksiUtama.getGrupTransaksi();
		if (parentCode == null) {
			Long milis = ais.ui.util.WaktuUtil.getDate().getTime() + (++ids);
			parentCode = "PARENT-" + Long.toHexString(milis).toUpperCase();
			transaksiUtama.setParentCode(parentCode);
			// Hanya buat grup baru bila memang belum ada (entri baru). Untuk jurnal lama yang
			// parentCode-nya null (dibuat modul lain), PERTAHANKAN grup asli agar baris akun tampil.
			if (grupTransaksi == null || grupTransaksi.getId() == null) {
				grupTransaksi = new GrupTransaksi();
				grupTransaksi.setJenisJurnal(Transaksi.JURNAL_KAS_MASUK);
			}
		}

		transaksiUtama.setJenisJurnal(Transaksi.JURNAL_KAS_MASUK);
		setWidth("80%");
		setHeight("90%");
		setClosable(false);
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(this);
		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(this);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Jurnal Penerimaan Kas / Bank");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("320px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		Columns columns = new Columns();

		columns.setParent(searchgrid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		// column.setWidth("35%");

		// column = new MyColumnConfig();
		// column.setParent(columns);
		// column.setLabel("");
		// column.setWidth("15%");
		//
		// column = new MyColumnConfig();
		// column.setParent(columns);
		// column.setLabel("");
		// column.setWidth("35%");

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal"));
		row.appendChild(
				tanggal = new MyDatebox(transaksiUtama.getTanggalTransaksi() == null ? ais.ui.util.WaktuUtil.getDate()
						: transaksiUtama.getTanggalTransaksi()));
		tanggal.setFormat(Common.dateFormat.get().toPattern());
		tanggal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Nomor Jurnal"));
		row.appendChild(noref = new Label(grupTransaksi.getKode() == null
				? CommonAkunting.generateNoJurnal(grupTransaksi.getJenisTransaksi(), false)
				: grupTransaksi.getKode()));

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Akun Kas / Bank"));
		row.appendChild(kodeAkun = new AmbilDataAkunDebetBanbox());
		kodeAkun.setWidth("90%");
		kodeAkun.setValue(transaksiUtama.getAkun() == null ? "" : transaksiUtama.getAkun().getNama());
		kodeAkun.setAttribute("akun", transaksiUtama.getAkun());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Satuan Kerja"));
		row.appendChild(satuanKerja = new AmbilDataSatuanKerjaBanbox(true));
		satuanKerja.setValue(grupTransaksi.getSatuanKerja() == null
				? (Common.getCurrentUser().ambilSatuanKerja() == null ? ""
						: Common.getCurrentUser().ambilSatuanKerja().toString())
				: grupTransaksi.getSatuanKerja().toString());
		satuanKerja.setAttribute("satuanKerja",
				grupTransaksi.getSatuanKerja() == null ? Common.getCurrentUser().ambilSatuanKerja()
						: grupTransaksi.getSatuanKerja());
		satuanKerja.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Nilai Transaksi"));
		row.appendChild(jumlah = new MyDoublebox(transaksiUtama.getDebet()));

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan Penerimaan"));

		row.appendChild(keteranganAkun = new Textbox(transaksiUtama.getKeterangan()));
		keteranganAkun.setRows(3);
		keteranganAkun.setWidth("100%");

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Data Transaksi", "/img/new.gif");
		toolbar.appendChild(button);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onAdd(event);
			}
		});

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(center);

		columns = new Columns();

		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kode Akun");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama Akun");
		// column.setWidth("15%");

		// column = new MyColumnConfig();
		// column.setParent(columns);
		// column.setLabel("Tanggal");
		// column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("25%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Divisi/Pegawai");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("10%");

		South south = new South();
		south.setParent(borderlayout);

		Foot foot = new Foot();
		foot.setParent(grid);

		foot.appendChild(new Footer());
		foot.appendChild(new Footer());
		foot.appendChild(new Footer());
		foot.appendChild(new Footer("Total Transaksi"));

		Footer footer = new Footer();
		footer.setParent(foot);
		labelTotal.setParent(footer);
		labelTotal.setStyle("font-weight:bold;font-size:15px;");

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		final MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		batal.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				Session session = HibernateUtil.currentSession();
				session.createSQLQuery(
						"delete from akunting.transaksi where simpan = false and parent_code = '" + parentCode + "'")
						.executeUpdate();
				List<Transaksi> transaksis = session.createCriteria(Transaksi.class)
						.add(Restrictions.eq("simpan", true)).add(Restrictions.eq("parentCode", parentCode)).list();

				if (transaksis.size() == 0 && grupTransaksi.getId() != null) {
					Common.refreshDelete((grupTransaksi));
				}

				if (eventListener != null) {
					eventListener.onEvent(event);
				}

				TransaksiJurnalPenerimaanHelper.this.detach();
			}
		});
		batal.setParent(toolbar);

		Session session = HibernateUtil.currentNativeSession();

		Integer countPosting = ((Number) session.createCriteria(Transaksi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("parentCode", parentCode))
				.add(Restrictions.eq("statusPosting", Transaksi.STATUS_POSTING_SELESAI)).uniqueResult()).intValue();

		Integer countAll = ((Number) session.createCriteria(Transaksi.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("parentCode", parentCode)).uniqueResult()).intValue();

		HibernateUtil.closeSession();

		save = new MyToolbarbuttonConfig("Selesai", "/img/save.gif");
		save.setVisible(edit);
		if (countAll != 0 && countPosting.equals(countAll)) {
			save.setVisible(false);
		}
		save.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				Session session = HibernateUtil.currentNativeSession();

				List<Transaksi> transaksis = session.createCriteria(Transaksi.class)
						.add(Restrictions.eq("parentCode", parentCode)).list();

				session.getTransaction().begin();
				for (Transaksi transaksi : transaksis) {
					transaksi.setSimpan(true);
					Common.refreshUpdate(session, transaksi);
				}
				session.getTransaction().commit();

				HibernateUtil.closeSession();

				onSaveTransaksiUtama(event);

				final Timer timer = new Timer(100);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						if (eventListener != null) {
							eventListener.onEvent(arg0);
						}
						TransaksiJurnalPenerimaanHelper.this.detach();
					}
				});
				timer.start();

			}
		});
		save.setParent(toolbar);

		MyToolbarbuttonConfig hapus = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
		hapus.setVisible(delete);
		if (countAll != 0 && countPosting.equals(countAll)) {
			hapus.setVisible(false);
		}
		hapus.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									Session session = HibernateUtil.currentSession();

									List<Transaksi> transaksis = session.createCriteria(Transaksi.class)
											.add(Restrictions.eq("parentCode", parentCode)).list();

									for (Transaksi transaksi : transaksis) {
										session.delete(transaksi);
									}

									if (grupTransaksi != null) {
										Common.refreshDelete((grupTransaksi));
									}

									if (eventListener != null) {
										eventListener.onEvent(event);
									}

									detach();

								}

							}
						});
			}
		});
		hapus.setParent(toolbar);

		onSearchDefault(null);

		if (!Common.getApakahAdmin()) {
			if (countAll != 0 && countPosting.equals(countAll)) {
				Common.freeze(this, true);
				batal.setDisabled(false);
			}

			if (grupTransaksi != null && grupTransaksi.getPostingHistory() != null) {
				Timer timer = new Timer(100);
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						MyMessageboxConfig.show(
								"Transaksi ini sudah di posting.. Anda tidak bisa mengubah data transaksi ini !",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						Common.freeze(TransaksiJurnalPenerimaanHelper.this, true);
						batal.setDisabled(false);
					}
				});
				timer.start();
			}
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				hitungTotal();
			}
		});
	}

	private class TransaksiJurnalPenerimaanEditor {
		private Transaksi transaksi;

		private TransaksiJurnalPenerimaanEditor(Transaksi transaksi) {
			this.transaksi = transaksi;
		}

		public boolean onSave(Event event) throws Exception {
			if (tanggal.getValue() == null) {
				MyMessageboxConfig.show("Mohon maaf, Tanggal Transaksi belum diisi. Langkah yang dapat dilakukan: (1) Pilih Tanggal Transaksi menggunakan date picker; (2) Pastikan tanggal yang dipilih valid dan sesuai periode akuntansi berjalan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (transaksi.getAkun() == null) {
				MyMessageboxConfig.show("Mohon maaf, Akun belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Akun melalui field pencarian akun yang tersedia; (2) Pastikan akun yang sesuai sudah terdaftar di master Akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (transaksi.getDebet() == null) {
				MyMessageboxConfig.show("Mohon maaf, Nilai belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nilai (Debet atau Kredit) dengan nominal yang valid; (2) Pastikan nilai tidak kosong; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (transaksi.getKredit() == null) {
				MyMessageboxConfig.show("Mohon maaf, Nilai belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nilai (Debet atau Kredit) dengan nominal yang valid; (2) Pastikan nilai tidak kosong; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (transaksi.getKredit() > 1.0 && transaksi.getDebet() > 1.0) {
				MyMessageboxConfig.show("Mohon maaf, salah satu nilai Kredit atau Debet harus nol. Langkah yang dapat dilakukan: (1) Kosongkan kolom Debet jika ini transaksi Kredit, atau kosongkan Kredit jika ini transaksi Debet; (2) Pastikan hanya satu sisi yang bernilai; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (transaksi.getKredit() < 1.0 && transaksi.getDebet() < 1.0) {
				MyMessageboxConfig.show("Mohon maaf, salah satu nilai Kredit atau Debet harus bukan nol. Langkah yang dapat dilakukan: (1) Isikan nilai yang valid pada kolom Debet atau Kredit; (2) Pastikan setidaknya satu kolom bernilai lebih dari nol; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
				return false;
			}
			if (!validasiSimpan()) {
				return false;
			}
			if (grupTransaksi.getId() == null) {
				onSaveTransaksiUtama(event);
			}

			transaksi.setJumlahTransaksi(transaksi.getKredit() < 1.0 ? transaksi.getKredit() : transaksi.getDebet());

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.setTime(tanggal.getValue());
			transaksi.setBulan(calendar.get(Calendar.MONTH) + 1);
			transaksi.setTahun(calendar.get(Calendar.YEAR));
			transaksi.setTanggalDimasukkan(tanggal.getValue());
			transaksi.setTanggalTransaksi(tanggal.getValue());

			transaksi.setKode(noref.getValue().trim());
			transaksi.setMerupakanDebet(false);
			transaksi.setJenisJurnal(Transaksi.JURNAL_KAS_MASUK);
			transaksi.setGrupTransaksi(grupTransaksi);

			grupTransaksi.setKode(noref.getValue().trim());

			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			if (transaksi.getId() != null) {
				session.update(transaksi);
			} else {
				session.save(transaksi);
			}
			session.getTransaction().commit();

			HibernateUtil.closeSession();

			hitungTotal();
			return true;
		}
	}

	class TransaksiRenderer extends ais.ui.util.MyRowRenderer {

		private void initNotEdit(final Row arg0, final Object arg1) throws Exception {
			Common.clear(arg0);
			final Transaksi transaksi = (Transaksi) arg1;
			transaksi.setParentCode(parentCode);
			RevisiHelper.createNewRevisi(Transaksi.class, transaksi,
					transaksi.getAkun() == null ? "" : transaksi.getAkun().getKode()).setParent(arg0);
			new Label(transaksi.getAkun() == null ? "" : transaksi.getAkun().getNama()).setParent(arg0);
			// new Label(
			// transaksi.getTanggalTransaksi() == null ? ""
			// : Common.dateFormat3.get().format(transaksi
			// .getTanggalTransaksi())).setParent(arg0);
			new Label(transaksi.getKeterangan()).setParent(arg0);
			new Label(transaksi.getPegawai() == null ? "" : transaksi.getPegawai().getNama()).setParent(arg0);
			new Label(transaksi.getKredit() == null ? "0" : Common.numberFormat.get().format(transaksi.getKredit()))
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.setAttribute("janganDisabled", true);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					initEdit(arg0, arg1);
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Question",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(transaksi);

											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			toolbar.setParent(arg0);
		}

		private void initEdit(final Row arg0, final Object arg1) throws Exception {
			Common.clear(arg0);
			final Transaksi transaksi = (Transaksi) arg1;
			final TransaksiJurnalPenerimaanEditor transaksiJurnalPenerimaanEditor = new TransaksiJurnalPenerimaanEditor(
					transaksi);
			transaksi.setParentCode(parentCode);
			transaksi.setJenisJurnal(Transaksi.JURNAL_KAS_MASUK);
			transaksi.setTanggalTransaksi(tanggal.getValue());

			final AmbilDataAkunKreditBanbox akunBanbox = new AmbilDataAkunKreditBanbox();
			akunBanbox.setValue(transaksi.getAkun() == null ? "" : transaksi.getAkun().getKode());
			akunBanbox.setAttribute("akun", transaksi.getAkun());
			akunBanbox.setParent(arg0);

			final Label namaAkun;
			(namaAkun = new Label(transaksi.getAkun() == null ? "" : transaksi.getAkun().getNama())).setParent(arg0);

			akunBanbox.setEventListener(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Akun akun = (Akun) akunBanbox.getAttribute("akun");
					namaAkun.setValue(akun == null ? "" : akun.getNama());
					transaksi.setAkun(akun);
				}
			});
			akunBanbox.setWidth("90%");

			// final Datebox tanggal = new MyDatebox(
			// transaksi.getTanggalTransaksi() == null ? ais.ui.util.WaktuUtil.getDate()
			// : transaksi.getTanggalTransaksi());
			// tanggal.setFormat(Common.dateFormat.get().toPattern());
			// tanggal.setParent(arg0);
			// tanggal.addEventListener("onChange", new EventListener() {
			//
			// @Override
			// public void onEvent(Event arg0) throws Exception {
			// transaksi.setTanggalTransaksi(tanggal.getValue());
			//
			// }
			// });
			// tanggal.setWidth("90%");

			final Textbox keterangan = new Textbox(transaksi.getKeterangan());
			keterangan.setParent(arg0);
			keterangan.setRows(3);
			keterangan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					transaksi.setKeterangan(keterangan.getValue().trim());
				}
			});
			keterangan.setWidth("90%");

			final AmbilDataPegawaiBanbox pegawaiBanbox = new AmbilDataPegawaiBanbox(true);
			pegawaiBanbox.setValue(transaksi.getPegawai() == null ? ""
					: (transaksi.getPegawai().getKode() == null ? "" : transaksi.getPegawai().getKode() + " - ")
							+ (transaksi.getPegawai().getNama()));
			pegawaiBanbox.setAttribute("pegawai", transaksi.getPegawai());
			pegawaiBanbox.setParent(arg0);

			pegawaiBanbox.setEventListener(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Pegawai pegawai = (Pegawai) pegawaiBanbox.getAttribute("pegawai");
					transaksi.setPegawai(pegawai);
				}
			});
			pegawaiBanbox.setWidth("90%");

			final Doublebox kredit = new MyDoublebox(transaksi.getKredit());
			kredit.setParent(arg0);
			kredit.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					transaksi.setKredit(kredit.getValue());
					transaksi.setDebet(0.0);
				}
			});
			kredit.setWidth("90%");
			kredit.setFormat("#,##0.##");
			kredit.setSclass("rightDisplay");

			Hbox toolbar = new Hbox();
			toolbar.setHeight("30px");
			toolbar.setParent(arg0);
			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (transaksi.getKode() != null) {
						initNotEdit(arg0, arg1);
					} else {
						Session session = HibernateUtil.currentSession();
						session.delete(arg1);
						onSearchDefault(null);
						arg0.detach();
					}

				}
			});
			cancel.setParent(toolbar);
			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					if (transaksiJurnalPenerimaanEditor.onSave(event)) {
						initNotEdit(arg0, transaksi);
					}
				}
			});
			save.setParent(toolbar);
		}

		@Override
		public void render(final Row arg0, final Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final Transaksi transaksi = (Transaksi) arg1;
			if (transaksi == null) {
				arg0.detach();
				return;
			}
			if (transaksi.getAkun() == null) {
				initEdit(arg0, arg1);
			} else {
				initNotEdit(arg0, arg1);
			}
		}

	}

	public void onAdd(Event event) throws Exception {
		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Transaksi belum diisi. Langkah yang dapat dilakukan: (1) Pilih Tanggal Transaksi menggunakan date picker; (2) Pastikan tanggal yang dipilih valid dan sesuai periode akuntansi berjalan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (kodeAkun.getAttribute("akun") == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Akun melalui field pencarian akun yang tersedia; (2) Pastikan akun yang sesuai sudah terdaftar di master Akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (noref.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nomor Jurnal belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nomor Jurnal dengan nomor yang valid dan unik; (2) Pastikan nomor tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		if (jumlah.getValue() == null || jumlah.getValue() < 1.0) {
			MyMessageboxConfig.show("Mohon maaf, Nilai Debet tidak boleh nol. Langkah yang dapat dilakukan: (1) Isikan kolom Nilai Debet dengan nominal yang lebih dari nol; (2) Pastikan nilai transaksi penerimaan sudah benar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			jumlah.focus();
			return;
		}
		if (keteranganAkun.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Keterangan belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Keterangan dengan deskripsi transaksi penerimaan yang jelas; (2) Pastikan keterangan tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return;
		}
		Transaksi transaksi = new Transaksi();
		transaksi.setMerupakanDebet(false);
		transaksi.setAkun(null);

		transaksi.setParentCode(parentCode);
		transaksi.setJenisJurnal(Transaksi.JURNAL_KAS_MASUK);
		transaksi.setKredit(jumlah.getValue());
		transaksi.setKeterangan(keteranganAkun.getValue());
		Session session = HibernateUtil.currentSession();
		session.save(transaksi);
		newTransaksis.add(transaksi);
		onSearchDefault(null);
	}

	public boolean validasiSimpan() throws Exception {
		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, Tanggal Transaksi belum diisi. Langkah yang dapat dilakukan: (1) Pilih Tanggal Transaksi menggunakan date picker; (2) Pastikan tanggal yang dipilih valid dan sesuai periode akuntansi berjalan; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (noref.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon maaf, Nomor Jurnal belum diisi. Langkah yang dapat dilakukan: (1) Isikan kolom Nomor Jurnal dengan nomor yang valid dan unik; (2) Pastikan nomor tidak kosong atau hanya terdiri dari spasi; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (jumlah.getValue() == null || jumlah.getValue() < 1.0) {
			MyMessageboxConfig.show("Mohon maaf, Nilai Debet tidak boleh nol. Langkah yang dapat dilakukan: (1) Isikan kolom Nilai Debet dengan nominal yang lebih dari nol; (2) Pastikan nilai transaksi penerimaan sudah benar; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			jumlah.focus();
			return false;
		}
		if (grid == null || grid.getRows() == null || grid.getRows().getChildren() == null
				|| grid.getRows().getChildren().size() == 0) {
			MyMessageboxConfig.show("Mohon maaf, Detail Transaksi belum diisi. Langkah yang dapat dilakukan: (1) Tambahkan minimal satu baris detail transaksi penerimaan menggunakan tombol Tambah; (2) Lengkapi kolom akun dan nilai pada setiap baris detail; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		Akun akun = (Akun) kodeAkun.getAttribute("akun");
		if (akun == null) {
			MyMessageboxConfig.show("Mohon maaf, Akun utama belum dipilih. Langkah yang dapat dilakukan: (1) Pilih Akun utama melalui field pencarian akun yang tersedia; (2) Pastikan akun yang sesuai sudah terdaftar di master Akun; (3) ulangi proses simpan. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		return true;
	}

	public boolean onSaveTransaksiUtama(Event event) throws Exception {

		if (!validasiSimpan()) {
			return false;
		}

		Session session = HibernateUtil.currentNativeSession();
		Transaksi transaksiUtama;
		if (this.transaksiUtama.getId() != null) {
			transaksiUtama = (Transaksi) session.createCriteria(Transaksi.class)
					.add(Restrictions.idEq(this.transaksiUtama.getId())).uniqueResult();
		} else {
			transaksiUtama = this.transaksiUtama;
		}

		GrupTransaksi grupTransaksi;
		if (this.grupTransaksi.getId() != null) {
			grupTransaksi = (GrupTransaksi) session.createCriteria(GrupTransaksi.class)
					.add(Restrictions.idEq(this.grupTransaksi.getId())).uniqueResult();
		} else {
			grupTransaksi = this.grupTransaksi;
		}

		transaksiUtama.setTanggalTransaksi(tanggal.getValue());
		if (grupTransaksi.getKode() == null || grupTransaksi.getKode().trim().equals("")) {
			grupTransaksi.setKode(CommonAkunting.generateNoJurnal(grupTransaksi.getJenisTransaksi(), true));
		} else {
			grupTransaksi.setKode(noref.getValue());
		}
		Akun akun = (Akun) kodeAkun.getAttribute("akun");
		transaksiUtama.setAkun(akun);

		transaksiUtama.setDebet(jumlah.getValue() == null ? 0.0 : jumlah.getValue());
		transaksiUtama.setKredit(0.0);

		transaksiUtama.setKeterangan(keteranganAkun.getValue().trim());

		transaksiUtama.setJumlahTransaksi(
				transaksiUtama.getKredit() < 1.0 ? transaksiUtama.getKredit() : transaksiUtama.getDebet());

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.setTime(tanggal.getValue());
		transaksiUtama.setBulan(calendar.get(Calendar.MONTH) + 1);
		transaksiUtama.setTahun(calendar.get(Calendar.YEAR));
		transaksiUtama.setTanggalDimasukkan(tanggal.getValue());
		transaksiUtama.setTanggalTransaksi(tanggal.getValue());

		transaksiUtama.setKode(noref.getValue());
		transaksiUtama.setMerupakanDebet(true);
		transaksiUtama.setJenisJurnal(Transaksi.JURNAL_KAS_MASUK);
		transaksiUtama.setSimpan(true);

		grupTransaksi.setSatuanKerja((SatuanKerja) satuanKerja.getAttribute("satuanKerja"));

		grupTransaksi.setKeterangan(keteranganAkun.getValue());
		grupTransaksi.setKode(noref.getValue());
		grupTransaksi.setTbmuser(Common.getCurrentUser());
		grupTransaksi.setTanggalTransaksi(tanggal.getValue());
		grupTransaksi.setParentCode(parentCode);

		grupTransaksi.setTotalKredit(total);
		grupTransaksi.setTotalDebet(jumlah.getValue());

		transaksiUtama.setGrupTransaksi(grupTransaksi);

		session.getTransaction().begin();
		if (transaksiUtama.getId() != null) {
			session.update((grupTransaksi));
			session.update(transaksiUtama);
		} else {
			session.save(grupTransaksi);
			session.save(transaksiUtama);
		}

		session.getTransaction().commit();

		HibernateUtil.closeSession();

		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = HibernateUtil.currentSession();
		org.hibernate.Criteria criteria = session.createCriteria(Transaksi.class).addOrder(Order.desc("id"))
				.add(Restrictions.eq("merupakanDebet", false))
				.add(Restrictions.eq("jenisJurnal", Transaksi.JURNAL_KAS_MASUK));
		if (grupTransaksi != null && grupTransaksi.getId() != null) {
			// PERBAIKAN konsisten dgn Jurnal Umum: jurnal lama bisa ber-parentCode null/beda,
			// muat baris akun lewat FK grupTransaksi ATAU parentCode (baris baru belum tersimpan).
			criteria.add(Restrictions.or(Restrictions.eq("grupTransaksi", grupTransaksi),
					Restrictions.eq("parentCode", parentCode)));
		} else {
			criteria.add(Restrictions.eq("parentCode", parentCode));
		}
		List<Transaksi> transaksi = criteria.list();
		if (transaksi.size() > 10) {
			grid.setMold("paging");
			grid.setPageSize(10);
			grid.getPagingChild().setMold("os");
		} else {
			grid.setMold("default");
		}
		ListModel strset = new SimpleListModel(transaksi);
		grid.setRowRenderer(new TransaksiRenderer());
		grid.setModelCheckMobile(strset);

		hitungTotal();

	}

	@SuppressWarnings("unchecked")
	private void hitungTotal() {
		total = 0.0;
		if (grid != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
			List<Row> rows = grid.getRows().getChildren();
			for (Row row : rows) {
				try {
					Component component = (Component) row.getChildren().get(4);
					if (component instanceof Label) {
						Label label = (Label) component;
						Double n = 0.0;
						try {
							n = (Double) Common.numberFormat.get().parse(label.getValue()).doubleValue();
						} catch (Exception e) {
							Common.tampilErrorJikaAdmin(e);
						}
						total += n;
					} else if (component instanceof Doublebox) {
						Doublebox label = (Doublebox) component;
						Double n = label.getValue();
						total += n;
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/akunting/helper/TransaksiJurnalPenerimaanHelper.java:966");

				}
			}
		}
		grupTransaksi.setTotalKredit(total);
		grupTransaksi.setTotalDebet(jumlah.getValue());
		labelTotal.setValue(Common.numberFormat.get().format(total));
		if (grupTransaksi.getTotalDebet().toString().equals("0.0")
				|| grupTransaksi.getTotalKredit().toString().equals("0.0")
				|| !grupTransaksi.getTotalDebet().toString().equals(grupTransaksi.getTotalKredit().toString())) {
			save.setDisabled(true);
		} else {
			save.setDisabled(false);
		}
	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}

}
