package ais.action.master.sirs;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.RowRenderer;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.sirs.util.CommonPendaftaranUtil;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.CommonSirs;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.asset.Lokasi;
import ais.database.model.sirs.Asuransi;
import ais.database.model.sirs.BookingRegistrasi;
import ais.database.model.sirs.Dokter;
import ais.database.model.sirs.JadwalDokter;
import ais.database.model.sirs.JenisPasien;
import ais.database.model.sirs.Pasien;
import ais.database.model.sirs.Poly;
import ais.database.model.sirs.Shift;
import ais.database.model.sirs.Tindakan;
import ais.ui.util.MyTextbox;

public class BookingRegistrasiAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Tabpanel tambahData;
	private Grid grid;
	private Paging paging;

	private MyTextbox searchkode;
	private MyTextbox searchnama;
	private MyTextbox searchdokter;
	private MyTextbox searchmr;
	private MyTextbox searchtelp;
	private MyTextbox searchalamat;

	private Combobox searchPaket;
	private Combobox searchjenisPasien;

	private Label kode;
	private Pasien pasien;
	private Asuransi asuransi;
	private Date tanggalBookingRegistrasi;
	private String keterangan;
	private Date dilayaniTanggal;
	private Poly poly;
	private Poly subpoly;
	private Dokter dokter;

	private Checkbox baru;

	private boolean edit = false;
	private boolean delete = false;

	private BookingRegistrasi bookingRegistrasi;
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

		Comboitem comboitem = new Comboitem("Ya");
		if (comboitem != null) { comboitem.setValue(true); }
		searchPaket.appendChild(comboitem);
		comboitem = new Comboitem("Tidak");
		if (comboitem != null) { comboitem.setValue(false); }
		searchPaket.appendChild(comboitem);

		Common.insertCombo(searchjenisPasien, "nama", JenisPasien.class);

		add = new ais.ui.util.MyToolbarbuttonConfig("Booking Baru", "/img/user_male_add.png");
		add.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				init(new BookingRegistrasi());
			}
		});
		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		init(new BookingRegistrasi());
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
	}

	class BookingRegistrasiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			// TODO Auto-generated method stub
			final BookingRegistrasi bookingRegistrasi = (BookingRegistrasi) arg1;
			if (bookingRegistrasi.getPasien() == null) {
				arg0.detach();
				return;
			}

			Pasien pasien = bookingRegistrasi.getPasien();
			if (pasien.getAktif() == null || !pasien.getAktif()) {
				arg0.setStyle("background-color:red;");
			}

			new Label(bookingRegistrasi.getKode()).setParent(arg0);
			new Label(pasien.getKode()).setParent(arg0);
			RevisiHelper.createNewRevisi(BookingRegistrasi.class, bookingRegistrasi, pasien.getNama()).setParent(arg0);
			new Label(Common.dateFormat3.get().format(bookingRegistrasi.getTanggalBookingRegistrasi())).setParent(arg0);

			new Label(bookingRegistrasi.getPoly() == null ? ""
					: bookingRegistrasi.getPoly().getNama() + (bookingRegistrasi.getSubpoly() == null ? ""
							: " (" + bookingRegistrasi.getSubpoly().getNama() + ")"))
					.setParent(arg0);

			new Label(pasien.getAlamatLengkap()).setParent(arg0);

			new Label(bookingRegistrasi.getDokter() == null ? "" : bookingRegistrasi.getDokter().getNama())
					.setParent(arg0);
			new Label(Common.dateFormat2.get().format(bookingRegistrasi.getBookingUntukTanggal())).setParent(arg0);
			new Label(bookingRegistrasi.getMerupakanPaket() ? "Ya" : "Tidak").setParent(arg0);
			new Label(pasien == null ? "" : pasien.getNoTelp() + " / " + pasien.getNoHp()).setParent(arg0);
			new Label(pasien == null ? "" : pasien.getJenisPasien() == null ? "" : pasien.getJenisPasien().getNama())
					.setParent(arg0);

			new Label(pasien.getKeterangan()).setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setVisible(pasien.getAktif() != null && pasien.getAktif());
			Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/edit.gif");
			button.setTooltiptext("Rubah Data");
			button.setVisible(edit && (bookingRegistrasi.getPendaftaran() == null));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					BookingRegistrasi bookingRegistrasiEdit = (BookingRegistrasi) HibernateUtil.currentSession()
							.createCriteria(BookingRegistrasi.class).add(Restrictions.idEq(bookingRegistrasi.getId()))
							.uniqueResult();

					init(bookingRegistrasiEdit);
				}

			});
			button.setParent(toolbar);

			button = new ais.ui.util.MyToolbarbuttonConfig("", "/img/delete.gif");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete && (bookingRegistrasi.getPendaftaran() == null));
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					onDelete(bookingRegistrasi);
				}
			});
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);
		}

	}

	public void onDelete(final BookingRegistrasi bookingRegistrasi) throws Exception {
		MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin menghapus data booking registrasi ini? Data yang sudah dihapus tidak dapat dikembalikan.", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = new Integer(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							try {
								Common.refreshDelete(bookingRegistrasi);
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

	public void onAdd(Event event) throws Exception {
		init(new BookingRegistrasi());
	}

	private EventListener perubahanPasienListener;
	private Set<Tindakan> pakets;
	private East myEaes;
	private Checkbox merupakanPaket;
	protected JadwalDokter shiftDokter;

	@SuppressWarnings("unchecked")
	private void initPilihanPaket(East myEast, final BookingRegistrasi bookingRegistrasi) {
		Common.clear(myEast);

		pakets = bookingRegistrasi.getPakets();

		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");
		borderlayout.setParent(myEast);

		Center center = new Center();
		center.setTitle("Pilih paket perawatan");
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		final East east = new East();
		east.setWidth("60%");
		east.setParent(borderlayout);

		Grid grid = new Grid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Kode");
		column.setWidth("30%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("Nama Paket");

		List<Tindakan> tindakans = ConstantValues.simpleList(HibernateUtil.currentSession()
				.createCriteria(Tindakan.class).add(Restrictions.eq("jenisPaket", Tindakan.JENIS_PERAWATAN_PAKET))
				.addOrder(Order.asc("nama")).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))), Tindakan.class);

		Rows rows = new Rows();
		rows.setParent(grid);

		for (final Tindakan tindakan : tindakans) {
			Row row = new Row();
			row.setParent(rows);

			final Checkbox paket = new Checkbox();
			for (Tindakan t : pakets) {
				if (t.getId().equals(tindakan.getId())) {
					paket.setChecked(true);
					break;
				}
			}
			paket.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (paket.isChecked()) {
						pakets.add(tindakan);
					} else {
						for (Tindakan t : pakets) {
							if (t.getId().equals(tindakan.getId())) {
								pakets.remove(t);
								break;
							}
						}
					}
					BookingRegistrasiAction.this.bookingRegistrasi.setPakets(pakets);
					CommonPendaftaranUtil.displayDetailPaket(east, pakets);
				}
			});
			paket.setParent(row);
			row.appendChild(new Label(tindakan.getKode()));
			row.appendChild(new Label(tindakan.getNama()));
		}

		CommonPendaftaranUtil.displayDetailPaket(east, pakets);
	}

	@SuppressWarnings("deprecation")
	private void init(final BookingRegistrasi bookingRegistrasi) throws Exception {

		this.bookingRegistrasi = bookingRegistrasi;

		BookingRegistrasiAction.this.pasien = bookingRegistrasi.getPasien();
		BookingRegistrasiAction.this.tanggalBookingRegistrasi = bookingRegistrasi.getTanggalBookingRegistrasi();
		BookingRegistrasiAction.this.asuransi = bookingRegistrasi.getAsuransi();
		BookingRegistrasiAction.this.keterangan = bookingRegistrasi.getKeterangan();

		BookingRegistrasiAction.this.poly = bookingRegistrasi.getPoly();
		BookingRegistrasiAction.this.subpoly = bookingRegistrasi.getSubpoly();
		BookingRegistrasiAction.this.shiftDokter = bookingRegistrasi.getJadwalDokter();
		BookingRegistrasiAction.this.dokter = bookingRegistrasi.getDokter();
		BookingRegistrasiAction.this.dilayaniTanggal = bookingRegistrasi.getDilayaniTanggal();

		BookingRegistrasiAction.this.myLokasi = bookingRegistrasi.getLokasi();
		BookingRegistrasiAction.this.myShift = bookingRegistrasi.getShift();

		Common.clear(tambahData);
		Borderlayout borderlayout = new Borderlayout();
		borderlayout.setWidth("100%");
		borderlayout.setHeight("100%");

		myEaes = new East();
		myEaes.setWidth("60%");
		myEaes.setParent(borderlayout);
		myEaes.setVisible(bookingRegistrasi.getMerupakanPaket());

		initPilihanPaket(myEaes, bookingRegistrasi);

		Center west = new Center();
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setTitle("Input data pasien");

		Grid grid = new Grid();
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();

		columns.setParent(grid);

		Column column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("20%");

		column = new Column();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("30%");

		Rows rows = new Rows();
		rows.setParent(grid);

		final Button tambahPasienBaru = new ais.ui.util.MyToolbarbuttonConfig("Pasien Baru", "/img/user_male.png");

		Row row = new Row();
		ais.ui.util.ZkCompat.setSpans(row, "1,3");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Merupakan Pasien Baru")));

		Hbox hbox = new Hbox();
		row.appendChild(hbox);
		hbox.appendChild(baru = new Checkbox());
		baru.setChecked(bookingRegistrasi.getBaru() == null ? null : bookingRegistrasi.getBaru());
		baru.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				tambahPasienBaru.setDisabled(!baru.isChecked());
			}
		});

		tambahPasienBaru.setDisabled(true);
		hbox.appendChild(tambahPasienBaru);
		tambahPasienBaru.addEventListener(Events.ON_CLICK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				PasienAction.onExternalAdd(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						final Window window = (Window) arg0.getTarget();
						final Pasien pasien = (Pasien) arg0.getData();
						perubahanPasienListener.onEvent(new Event("", tambahPasienBaru, pasien));
						window.detach();
					}
				});

			}
		});

		kode = new Label(bookingRegistrasi.getKode());
		perubahanPasienListener = CommonPendaftaranUtil.initBookingRegistrasi(rows, kode, bookingRegistrasi,
				new EventListener() {

					@SuppressWarnings("rawtypes")
					@Override
					public void onEvent(Event arg0) throws Exception {
						Map data = (Map) arg0.getData();
						BookingRegistrasi bookingRegistrasi = (BookingRegistrasi) data.get("bookingRegistrasi");
						pasien = bookingRegistrasi.getPasien();
						tanggalBookingRegistrasi = bookingRegistrasi.getTanggalBookingRegistrasi();
						asuransi = bookingRegistrasi.getAsuransi();
						keterangan = bookingRegistrasi.getKeterangan();
						initPilihanPaket(myEaes, bookingRegistrasi);
					}
				});

		row = new Row();
		row.setAttribute("hide", "no");
		ais.ui.util.ZkCompat.setSpans(row, "4");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Html("<hr>"));

		row = new Row();
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Merupakan Paket")));
		row.appendChild(merupakanPaket = new Checkbox());
		merupakanPaket.setChecked(bookingRegistrasi.getMerupakanPaket());
		merupakanPaket.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				myEaes.setVisible(merupakanPaket.isChecked());
			}
		});

		CommonPendaftaranUtil.initJadwalPemeriksaan(rows, bookingRegistrasi, null, new EventListener() {

			@SuppressWarnings("rawtypes")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Map data = (Map) arg0.getData();
				if (data != null) {
					poly = (Poly) data.get("poly");
					subpoly = (Poly) data.get("subpoly");
					shiftDokter = (JadwalDokter) data.get("jadwalDokter");
					dokter = (Dokter) data.get("dokter");
					dilayaniTanggal = (Date) data.get("dilayaniTanggal");
				} else {
					poly = null;
					subpoly = null;
					shiftDokter = null;
					dokter = null;
					dilayaniTanggal = null;
				}
			}
		});

		CommonSirs.initLokasiDanShift(bookingRegistrasi.getLokasi() == null ? myLokasi : bookingRegistrasi.getLokasi(),
				bookingRegistrasi.getShift(), rows, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Object[] o = (Object[]) arg0.getData();
						myLokasi = (Lokasi) o[0];
						myShift = (Shift) o[1];

						if (kode.getValue().trim().equals("") && myLokasi != null) {
							String mykode = Common.generateCode(BookingRegistrasi.class, 10, "REG-BOOK", myLokasi);
							kode.setValue(mykode);
						}

					}
				});

		row = new Row();
		row.setAttribute("hide", "no");
		row.setStyle("border:0px;background: transparent;");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "4");

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(row);

		add.setParent(toolbar);
		Toolbarbutton save = new ais.ui.util.MyToolbarbuttonConfig("Simpan Booking Pasien ", "/img/save.gif");
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

		Toolbarbutton button = new ais.ui.util.MyToolbarbuttonConfig("Batalkan Booking Pasien ", "/img/delete.gif");
		button.setVisible(delete);
		button.setTooltiptext("Batalkan Booking Registrasi Pasien ");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (bookingRegistrasi != null && bookingRegistrasi.getId() != null) {
					onDelete(bookingRegistrasi);

				} else {
					MyMessageboxConfig.show("Apakah Bapak/Ibu yakin ingin membatalkan booking registrasi pasien ini?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = new Integer(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										init(new BookingRegistrasi());
									}
								}
							});

				}
			}
		});
		button.setParent(toolbar);

		borderlayout.setParent(tambahData);
		tambahData.getLinkedTab().setSelected(true);

		if (kode.getValue().trim().equals("") && myLokasi != null) {
			kode.setValue(Common.generateCode(BookingRegistrasi.class, 10, "REG-BOOK", myLokasi));
		}

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

		if (tanggalBookingRegistrasi == null) {
			MyMessageboxConfig.show("Mohon maaf, tanggal booking wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) tentukan tanggal booking pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}
		if (pasien == null) {
			MyMessageboxConfig.show("Mohon maaf, pasien wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih pasien pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (poly == null) {
			MyMessageboxConfig.show("Mohon maaf, poli wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih poli pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (dokter == null) {
			MyMessageboxConfig.show("Mohon maaf, dokter wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih dokter pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (merupakanPaket.isChecked() && pakets.isEmpty()) {
			MyMessageboxConfig.show("Mohon maaf, untuk pendaftaran paket, Bapak/Ibu wajib memilih salah satu paket terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih salah satu paket yang tersedia; (2) atau hilangkan tanda pada pilihan pendaftaran paket apabila bukan pendaftaran paket.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (shiftDokter == null) {
			MyMessageboxConfig.show("Mohon maaf, shift tenaga medis wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) pilih shift tenaga medis pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (dilayaniTanggal == null) {
			MyMessageboxConfig.show("Mohon maaf, tanggal pelayanan wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) tentukan tanggal pelayanan pada kolom yang tersedia; (2) kemudian simpan kembali data Bapak/Ibu.", "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (bookingRegistrasi.getId() != null) {
			bookingRegistrasi = (BookingRegistrasi) session.load(BookingRegistrasi.class, bookingRegistrasi.getId());

		}
		if (kode.getValue().trim().equals("")) {
			kode.setValue(Common.generateCode(BookingRegistrasi.class, 8));
		}

		bookingRegistrasi.setDilayaniTanggal(dilayaniTanggal);
		bookingRegistrasi.setJadwalDokter(shiftDokter);

		bookingRegistrasi.setAsuransi((Asuransi) asuransi);

		bookingRegistrasi.setSubpoly((Poly) (subpoly));

		bookingRegistrasi.setDokter((Dokter) dokter);
		bookingRegistrasi.setPoly((Poly) (poly));
		bookingRegistrasi.setBaru(baru.isChecked());

		bookingRegistrasi.setTanggalBookingRegistrasi(tanggalBookingRegistrasi);
		bookingRegistrasi.setMerupakanPaket(merupakanPaket.isChecked());

		bookingRegistrasi.setPasien((Pasien) pasien);
		bookingRegistrasi.setPasienKomunitas(bookingRegistrasi.getPasien());

		bookingRegistrasi.setKode(kode.getValue());
		bookingRegistrasi.setKeterangan(keterangan);

		bookingRegistrasi.setPakets(new HashSet<Tindakan>());
		bookingRegistrasi.getPakets().addAll(pakets);
		bookingRegistrasi.setBookingUntukTanggal(bookingRegistrasi.getDilayaniTanggal());

		bookingRegistrasi.setLokasi(myLokasi);
		bookingRegistrasi.setShift(myShift);

		Integer antrian = CommonPendaftaranUtil.generateNomorAntrian(bookingRegistrasi,
				bookingRegistrasi.getJadwalDokter());
		bookingRegistrasi.setNomorAntrian(antrian);

		if (bookingRegistrasi.getId() != null) {
			Common.refreshUpdate(session, bookingRegistrasi);
		} else {
			bookingRegistrasi.setIndex(Common.generateMaxByLokasi(BookingRegistrasi.class, myLokasi) + 1);
			String mykode = Common.generateCode(BookingRegistrasi.class, 10, "REG-BOOK", myLokasi);
			kode.setValue(mykode);
			bookingRegistrasi.setKode(mykode);
			session.save(bookingRegistrasi);
		}

		MyMessageboxConfig.show("Data booking registrasi telah berhasil disimpan. Terima kasih, Bapak/Ibu.", "Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
				new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.freeze(tambahData, true);
						add.setDisabled(false);
					}
				});

		return true;
	}

	private Criteria initCriteria(boolean order) {

		Criterion criterion = searchalamat.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("pasien.alamat", searchalamat.getValue(), MatchMode.ANYWHERE);

		criterion = searchalamat.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(criterion,
						Restrictions.ilike("propinsi.nama", searchalamat.getValue(), MatchMode.ANYWHERE));

		criterion = searchalamat.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(criterion,
						Restrictions.ilike("kota.nama", searchalamat.getValue(), MatchMode.ANYWHERE));

		criterion = searchalamat.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(criterion,
						Restrictions.ilike("kecamatan.nama", searchalamat.getValue(), MatchMode.ANYWHERE));

		criterion = searchalamat.getValue().trim().equals("") ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(criterion,
						Restrictions.ilike("kelurahan.nama", searchalamat.getValue(), MatchMode.ANYWHERE));

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(BookingRegistrasi.class);

		if (order)
			criteria.addOrder(Order.desc("tanggalBookingRegistrasi"));

		criteria.createAlias("pasien", "pasien", Criteria.LEFT_JOIN).createAlias("dokter", "dokter", Criteria.LEFT_JOIN)

				.createAlias("pasien.propinsi", "propinsi", Criteria.LEFT_JOIN)
				.createAlias("pasien.kota", "kota", Criteria.LEFT_JOIN)
				.createAlias("pasien.kecamatan", "kecamatan", Criteria.LEFT_JOIN)
				.createAlias("pasien.kelurahan", "kelurahan", Criteria.LEFT_JOIN)

				.add(criterion)

				.add((searchtelp == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.or(Restrictions.ilike("pasien.noTelp", searchtelp.getValue(), MatchMode.ANYWHERE),
						Restrictions.ilike("pasien.noHp", searchtelp.getValue(), MatchMode.ANYWHERE))))

				.add((searchmr == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.kode", searchmr.getValue(), MatchMode.ANYWHERE)))
				.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("pasien.nama", searchnama.getValue(), MatchMode.ANYWHERE)))
				.add((searchdokter == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("dokter.nama", searchdokter.getValue(), MatchMode.ANYWHERE)))

				.add(searchPaket.getSelectedItem() != null
						? Restrictions.eq("merupakanPaket", searchPaket.getSelectedItem().getValue())
						: Restrictions.sqlRestriction("1=1"))

				.add(searchjenisPasien.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("pasien.jenisPasien", searchjenisPasien.getSelectedItem().getValue()))

				.add((searchkode == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (Restrictions.ilike("kode", searchkode.getValue(), MatchMode.ANYWHERE)));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<BookingRegistrasi> bookingRegistrasi = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(bookingRegistrasi);
		grid.setRowRenderer(new BookingRegistrasiRenderer());
		grid.setModel(strset);

		grid.renderAll();

	}

}
