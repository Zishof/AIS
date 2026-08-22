package ais.action.master.sirs;

import java.util.Calendar;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.helper.AmbilDataPendaftaranSemuaBanbox;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.DepositPasien;
import ais.database.model.sirs.JenisBiayaLain;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Pendaftaran;
import ais.database.model.sirs.Shift;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyTextbox;

public class DepositAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Tabpanel tambahData;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchnama;
	private MyTextbox searchkode;

	private MyDatebox tanggal;
	private AmbilDataPendaftaranSemuaBanbox pendaftaran;

	private Doublebox nilai;
	private MyTextbox keterangan;

	private boolean edit = false;
	private boolean delete = false;

	private Label nama;
	private Label umur;
	private Label alamat;
	private Label ttl;
	private Combobox jenisPasien;
	private Combobox caraBayarDepositPasien;
	private Label jenisKelamin;

	private DepositPasien deposit;
	private Toolbarbutton add;

	private Lokasi myLokasi = Common.getCurrentLokasi();
	private Shift myShift;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			execution.sendRedirect("/logoff");
			return;
		}

		add = new ais.ui.util.MyToolbarbuttonConfig("Buat DepositPasien Pasien", "/img/user_male_add.png");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				init(new DepositPasien());

			}
		});
		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		init(new DepositPasien());
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	public void onDelete(DepositPasien deposit, Session session) {
		session.delete(session.merge(deposit));
	}

	class DepositPasienRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final DepositPasien deposit = (DepositPasien) arg1;
			final Pasien pasien = deposit.getPendaftaran().getPasien();

			new Label(deposit.getTanggal() == null ? "" : Common.dateFormat3.get().format(deposit.getTanggal()))
					.setParent(arg0);
			RevisiHelper.createNewRevisi(DepositPasien.class, deposit, deposit.getPendaftaran().getPasien().getKode())
					.setParent(arg0);
			new Label(deposit.getPendaftaran().getPasien().getNama()).setParent(arg0);
			new Label(Common.numberFormat.get().format(deposit.getNilai())).setParent(arg0);

			deposit.hitungDiambil();

			new Label(Common.numberFormat.get().format(deposit.getDiambil())).setParent(arg0);

			Double sisaDepositPasien = deposit.getNilai() - deposit.getDiambil();

			new Label(Common.numberFormat.get().format(sisaDepositPasien)).setParent(arg0);

			new Label(pasien == null ? "" : pasien.getAlamatLengkap()).setParent(arg0);

			new Label(deposit.getJenisBiayaLain() == null ? "" : deposit.getJenisBiayaLain().getNama()).setParent(arg0);

			new Label(deposit.getCaraBayarDeposit() == null ? "" : deposit.getCaraBayarDeposit().getNama())
					.setParent(arg0);

			new Label(deposit.getPostingHistory() == null ? "Belum" : deposit.getPostingHistory().toString())
					.setParent(arg0);

			new Label(deposit.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit && deposit.getPostingHistory() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(deposit);

				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && deposit.getPostingHistory() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data ini? Data yang sudah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											Session session = HibernateUtil.currentSession();
											onDelete(deposit, session);
											onSearchDefault(event);
										} catch (Exception e) {
											ais.common.Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lainnya. Langkah yang dapat dilakukan: (1) periksa dan hapus terlebih dahulu data lain yang terkait dengan data ini; (2) pastikan tidak ada transaksi yang masih menggunakan data ini; (3) apabila kendala berlanjut, mohon hubungi administrator sistem. Rincian kesalahan: {V1}"
															, e.getMessage()));
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	private EventListener perubahanPasienListener = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			Pendaftaran pendaftaran = (Pendaftaran) DepositAction.this.pendaftaran.getAttribute("pendaftaran");
			if (pendaftaran == null) {
				return;
			}
			Pasien pasien = pendaftaran.getPasien();
			nama.setValue(pasien == null ? "" : pasien.getNama());

			if (pasien.getTanggalLahir() != null) {
				Calendar tahunSkr = Calendar.getInstance();
				Calendar tahunLahir = Calendar.getInstance();
				tahunLahir.setTime(pasien.getTanggalLahir());
				Integer myumur = tahunSkr.get(Calendar.YEAR) - tahunLahir.get(Calendar.YEAR);
				umur.setValue(myumur + " thn");
			} else {
				umur.setValue("");
			}

			alamat.setValue(pasien == null ? "" : pasien.getAlamatLengkap());
			ttl.setValue(pasien == null ? ""
					: (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + "/"
							+ (pasien.getTanggalLahir() == null ? ""
									: Common.dateFormat2.get().format(pasien.getTanggalLahir())));

			jenisKelamin
					.setValue(pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin());

			Common.selectComboItem(jenisPasien, pasien.getJenisPasien());

		}
	};
	private Window addWindow;
	private EventListener eventListener;
	private Combobox jenisBiayaLain;

	public void onAdd(Event event) throws Exception {
		init(new DepositPasien());
	}

	public static void addExternal(DepositPasien deposit, EventListener eventListener) throws Exception {
		DepositAction depositAction = new DepositAction();
		depositAction.addWindow = new Window();
		depositAction.addWindow.setTitle("Pendataan Deposit Pasien");
		depositAction.eventListener = eventListener;
		depositAction.addWindow.setHeight("400px");
		depositAction.addWindow.setWidth("650px");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(depositAction.addWindow);
		depositAction.init(deposit);
		depositAction.addWindow.setVisible(true);
		depositAction.addWindow.onModal();
		depositAction.pendaftaran.setDisabled(true);
	}

	private void init(DepositPasien deposit) throws Exception {
		this.deposit = deposit;

		Common.clear(tambahData == null ? addWindow : tambahData);
		Borderlayout borderlayout = new Borderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Ambil Pendaftaran Pasien")));
		row.appendChild(pendaftaran = new AmbilDataPendaftaranSemuaBanbox(false));
		pendaftaran.setAttribute("pendaftaran", deposit.getPendaftaran());
		pendaftaran.setValue(deposit.getPendaftaran() == null ? "" : deposit.getPendaftaran().getKode());
		pendaftaran.setWidth("90%");

		Pasien pasien = deposit.getPendaftaran() == null ? null : deposit.getPendaftaran().getPasien();

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label("Nama Pasien/Umur"));

		Hbox myHbox = new Hbox();
		row.appendChild(myHbox);
		myHbox.appendChild(nama = new Label(pasien == null ? "" : pasien.getNama()));
		myHbox.appendChild(new Label(" / "));
		myHbox.appendChild(
				umur = new Label(deposit.getPendaftaran() == null || deposit.getPendaftaran().getUmur() == null ? ""
						: deposit.getPendaftaran().getUmur() + " thn"));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Waktu DepositPasien")));
		row.appendChild(tanggal = new MyDatebox(deposit.getTanggal()));
		tanggal.setFormat(Common.dateFormat3.get().toPattern());
		tanggal.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Alamat")));
		row.appendChild(alamat = new Label(pasien == null ? "" : pasien.getAlamatLengkap()));

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("TTL")));
		row.appendChild(ttl = new Label(pasien == null ? ""
				: (pasien.getTempatLahir() == null ? "" : pasien.getTempatLahir()) + "/"
						+ (pasien.getTanggalLahir() == null ? ""
								: Common.dateFormat2.get().format(pasien.getTanggalLahir()))));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Kelamin")));
		row.appendChild(jenisKelamin = new Label(
				pasien == null ? "" : pasien.getJenisKelamin() == null ? "" : pasien.getJenisKelamin()));

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pasien")));
		Common.insertCombo(jenisPasien = new Combobox(), "nama", JenisPasien.class);
		Common.selectComboItem(jenisPasien,
				deposit.getPendaftaran() == null ? null : deposit.getPendaftaran().getJenisPasien());
		row.appendChild(jenisPasien);
		jenisPasien.setWidth("90%");
		jenisPasien.setDisabled(true);

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("DepositPasien digunakan untuk")));
		row.appendChild(jenisBiayaLain = new Combobox());
		Common.insertCombo(jenisBiayaLain, "nama", "akun", JenisBiayaLain.class,
				Restrictions.eq("jenis", JenisBiayaLain.SIMPAN_DEPOSIT));
		Common.selectComboItem(jenisBiayaLain, deposit.getJenisBiayaLain());
		jenisBiayaLain.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Cara Bayar DepositPasien")));
		row.appendChild(caraBayarDepositPasien = new Combobox());
		Common.insertCombo(caraBayarDepositPasien, "nama", "akun", JenisBiayaLain.class,
				Restrictions.eq("jenis", JenisBiayaLain.CARA_BAYAR_DEPOSIT));
		Common.selectComboItem(caraBayarDepositPasien, deposit.getCaraBayarDeposit());
		caraBayarDepositPasien.setWidth("90%");

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nilai Deposit Pasien")));
		row.appendChild(nilai = new MyDoublebox(deposit.getNilai()));
		nilai.setWidth("90%");

		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(deposit.getKeterangan() == null ? "" : deposit.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		CommonSirs.initLokasiDanShift(deposit.getLokasi() == null ? myLokasi : deposit.getLokasi(), deposit.getShift(),
				rows, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Object[] o = (Object[]) arg0.getData();
						myLokasi = (Lokasi) o[0];
						myShift = (Shift) o[1];
					}
				});

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(row);

		if (add != null) {
			add.setParent(toolbar);
		}
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan Data DepositPasien", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					Common.initPaging(paging, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSearchDefault(null);
						}
					});
				}
			}
		});
		save.setParent(toolbar);

		if (tambahData != null) {
			borderlayout.setParent(tambahData);
			tambahData.getLinkedTab().setSelected(true);
		} else {
			borderlayout.setParent(addWindow);
		}

		pendaftaran.setEventListener(perubahanPasienListener);
		perubahanPasienListener.onEvent(null);
	}

	public boolean onSave(Event event) throws Exception {

		if (myLokasi == null) {
			MyMessageboxConfig.show("Mohon maaf, lokasi wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih lokasi pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (myShift == null) {
			MyMessageboxConfig.show("Mohon maaf, shift wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih shift pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (pendaftaran.getAttribute("pendaftaran") == null) {
			MyMessageboxConfig.show("Mohon maaf, data pendaftaran pasien wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih data pendaftaran pasien pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (tanggal.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, tanggal keluar wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) tentukan tanggal keluar pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (nilai.getValue() == null) {
			MyMessageboxConfig.show("Mohon maaf, nilai deposit wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan nilai deposit pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (nilai.getValue() < 1) {
			MyMessageboxConfig.show("Mohon maaf, nilai deposit tidak boleh kurang dari 0. Langkah yang dapat dilakukan: (1) isikan nilai deposit dengan angka yang lebih besar atau sama dengan 0; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (jenisBiayaLain.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, jenis deposit pasien wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih jenis deposit pasien pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (caraBayarDepositPasien.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon maaf, cara bayar deposit pasien wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih cara bayar deposit pasien pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (deposit.getId() != null) {
			deposit = (DepositPasien) session.load(DepositPasien.class, deposit.getId());
		}

		deposit.setLokasi(myLokasi);
		deposit.setShift(myShift);

		deposit.setCaraBayarDeposit((JenisBiayaLain) caraBayarDepositPasien.getSelectedItem().getValue());
		deposit.setJenisBiayaLain((JenisBiayaLain) jenisBiayaLain.getSelectedItem().getValue());
		deposit.setTanggal(tanggal.getValue());
		deposit.setPendaftaran((Pendaftaran) pendaftaran.getAttribute("pendaftaran"));
		deposit.setKeterangan(keterangan.getValue());
		deposit.setNilai(nilai.getValue());

		Common.refreshSaveOrUpdate(session, deposit);

		MyMessageboxConfig.show("Data deposit telah berhasil disimpan. Terima kasih, Bapak/Ibu.", "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						if (eventListener != null) {
							eventListener.onEvent(new Event("event", null, deposit));
							addWindow.setVisible(false);
						}

						if (tambahData != null && add != null) {
							Common.freeze(tambahData, true);
							add.setDisabled(false);
						}

					}
				});

		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchkode == null || searchnama == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);
		List<DepositPasien> deposit = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(deposit);
		grid.setRowRenderer(new DepositPasienRenderer());
		grid.setModel(strset);

		grid.renderAll();

	}

	private Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(DepositPasien.class)
				.createAlias("pendaftaran", "pendaftaran", Criteria.INNER_JOIN)
				.createAlias("pendaftaran.pasien", "pasien", Criteria.INNER_JOIN)
				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.kode", searchkode.getValue(), MatchMode.ANYWHERE)))
				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.nama", searchnama.getValue(), MatchMode.ANYWHERE)));
		if (order)
			criteria.addOrder(Order.desc("tanggal"));

		return criteria;
	}
}
