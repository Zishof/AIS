package ais.action.master.pkl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataKelompokPklBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.ItemBiaya;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaDapatKelompokPkl;
import ais.database.model.Pkl;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.LampiranPklMahasiswa;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.pkl.MahasiswaDaftarPkl;
import ais.database.model.pkl.MahasiswaPklPersyaratan;
import ais.database.model.pkl.PersyaratanPkl;
import ais.database.model.pkl.PklPunyaPersyaratan;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyInclude;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextboxAngka;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class PklUntukMahasiswaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow daftar_pkl_window;
	private Paging paging;
	private MyGrid grid;

	// private MyToolbarbuttonConfig add;
	private Mahasiswa mahasiswa;

	// private Textbox no_SKTM;
	// private Textbox pejabat_penandatangan;

	protected Tabpanel pkl;

	public void onPKL(Event event) {

		if (pkl.getChildren().size() == 0) {
			MyInclude include = new MyInclude();
			include.setHeight("100%");
			include.setWidth("100%");
			include.setParent(pkl);
			include.setSrc("/pages/master/pkl/kelompok_pkl.zul");
		}
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		// if (session.getAttribute("usersTemp") == null
		// || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
		// session.removeAttribute("usersTemp");
		// Common.goLogoff();
		// return;
		// }
		//
		// add.setVisible(CommonPrivilages
		// .checkPrevilages(CommonPrivilages.CREATE));
		// add.setTooltiptext("Tambah");

		mahasiswa = Common.getCurrentUser().getMahasiswa();

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class PklRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Pkl pkl = (Pkl) arg1;

			RevisiHelper.createNewRevisi(Pkl.class, pkl, pkl.getNama_kelompok()).setParent(arg0);
			new Label(pkl.getTanggal_mulai() == null ? "" : Common.dateFormat4.get().format(pkl.getTanggal_mulai()))
					.setParent(arg0);
			new Label(pkl.getTanggal_selesai() == null ? "" : Common.dateFormat4.get().format(pkl.getTanggal_selesai()))
					.setParent(arg0);

			new Label(pkl.getFakultas() == null ? "Semua" : pkl.getFakultas().getNama()).setParent(arg0);
			new Label(pkl.getJurusan() == null ? "Semua" : pkl.getJurusan().getNama()).setParent(arg0);
			new Label(pkl.getProgram() == null || pkl.getProgram().trim().isEmpty() ? "Semua" : pkl.getProgram())
					.setParent(arg0);
			new Label(pkl.getMinimalSksBolehIkutPkl() + " SKS, IPK min " + pkl.getMinimalIpkBolehIkutPkl())
					.setParent(arg0);

			new Label(pkl.getAktifkanSyaratLain()
					? (pkl.getMinimalSksBolehIkutPkl2() + " SKS, IPK min " + pkl.getMinimalIpkBolehIkutPkl2())
					: "").setParent(arg0);

			final Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					if (!pkl.getKodeItemBiaya().trim().isEmpty()) {

						for (String kode : pkl.getKodeItemBiaya().trim().split(",")) {
							ItemBiaya itemBiaya = (ItemBiaya) session.createCriteria(ItemBiaya.class)
									.add(Restrictions.eq("kode", kode.trim())).setMaxResults(1).uniqueResult();
							if (itemBiaya != null) {
								new Label(itemBiaya.getKode() + "-" + itemBiaya.getNama()).setParent(hbox);
							}
						}
					} else {
						new Label("-").setParent(hbox);
					}
				}
			});

			Session session = HibernateUtil.currentSession();
			final MahasiswaDaftarPkl mahasiswaDaftarPkl = (MahasiswaDaftarPkl) session
					.createCriteria(MahasiswaDaftarPkl.class).setMaxResults(1)
					.add(Restrictions.eq("mahasiswa", Common.getCurrentUser().getMahasiswa()))
					.add(Restrictions.eq("pkl", pkl)).uniqueResult();

			Label lbl;
			(lbl = new Label(mahasiswaDaftarPkl == null ? "TIDAK TERDAFTAR"
					: mahasiswaDaftarPkl.getTerima().equals(MahasiswaDaftarPkl.BELUM_DIPROSES) ? "Belum Diproses"
							: mahasiswaDaftarPkl.getTerima().equals(MahasiswaDaftarPkl.DITERIMA) ? "DITERIMA"
									: "DITOLAK"))
					.setParent(arg0);

			if (mahasiswaDaftarPkl != null && mahasiswaDaftarPkl.getTerima().equals(MahasiswaDaftarPkl.DITERIMA)) {
				MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl = (MahasiswaDapatKelompokPkl) HibernateUtil
						.currentSession().createCriteria(MahasiswaDapatKelompokPkl.class).setMaxResults(1)
						.createAlias("kelompokPkl", "kelompokPkl").add(Restrictions.eq("kelompokPkl.pkl", pkl))
						.add(Restrictions.eq("mahasiswa", mahasiswa)).uniqueResult();

				if (mahasiswaDapatKelompokPkl == null || !mahasiswaDapatKelompokPkl.getDiterima()) {
					Hbox hb = new Hbox();
					hb.setParent(arg0);
					hb.appendChild(new Label(ais.common.Common.getBahasaConfig("Kelompok : ")));
					final AmbilDataKelompokPklBanbox ambil;
					hb.appendChild(ambil = new AmbilDataKelompokPklBanbox());
					ambil.setAttribute("kelompokPkl",
							mahasiswaDapatKelompokPkl == null ? null : mahasiswaDapatKelompokPkl.getKelompokPkl());
					ambil.setAttribute("myValue",
							mahasiswaDapatKelompokPkl == null ? null : mahasiswaDapatKelompokPkl.getKelompokPkl());
					ambil.setValue(
							mahasiswaDapatKelompokPkl == null || mahasiswaDapatKelompokPkl.getKelompokPkl() == null
									? null
									: mahasiswaDapatKelompokPkl.getKelompokPkl().getNama());
					ambil.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							KelompokPkl kelompokPkl = (KelompokPkl) ambil.getAttribute("kelompokPkl");

							MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl = (MahasiswaDapatKelompokPkl) HibernateUtil
									.currentSession().createCriteria(MahasiswaDapatKelompokPkl.class).setMaxResults(1)
									.createAlias("kelompokPkl", "kelompokPkl")
									.add(Restrictions.eq("kelompokPkl.pkl", pkl))
									.add(Restrictions.eq("mahasiswa", mahasiswa)).uniqueResult();
							if (mahasiswaDapatKelompokPkl == null) {
								mahasiswaDapatKelompokPkl = new MahasiswaDapatKelompokPkl();
							}
							mahasiswaDapatKelompokPkl.setMahasiswa(mahasiswa);
							mahasiswaDapatKelompokPkl.setKelompokPkl(kelompokPkl);
							Common.refreshSaveOrUpdate(mahasiswaDapatKelompokPkl);
						}
					});
				} else {
					lbl.setValue("DISETUJUI");
					arg0.appendChild(new Label(mahasiswaDapatKelompokPkl.getKelompokPkl().getNama()));
				}
			} else {
				arg0.appendChild(new Label());
			}

			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dikumpulkan lalu dibungkus
			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Daftar", "/img/svg/check2-circle.svg");
			button.setTooltiptext("Daftar");
			button.setOrient("vertical");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					init(pkl, false);
				}

			});
			aksiButtons.add(button);

			Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
			calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			Date sekarang = calendar.getTime();
			button.setDisabled(mahasiswaDaftarPkl != null || sekarang.after(pkl.getTanggal_selesai()));
			button.setVisible(!button.isDisabled());

			button = new MyToolbarbuttonConfig(mahasiswaDaftarPkl != null
					&& mahasiswaDaftarPkl.getTerima().equals(MahasiswaDaftarPkl.BELUM_DIPROSES) ? "Ubah" : "Lihat",
					"/img/absensi_pmb.png");
			button.setTooltiptext("Lihat");
			button.setOrient("vertical");
			button.setVisible(mahasiswaDaftarPkl != null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					init(pkl, !mahasiswaDaftarPkl.getTerima().equals(MahasiswaDaftarPkl.BELUM_DIPROSES));
				}

			});
			aksiButtons.add(button);

			MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak Bukti", "/img/print.png");
			cetak.setOrient("vertical");
			cetak.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("rawtypes")
				final Map parameters = ais.common.HashMapGenerator.getRand();

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					// TODO Auto-generated method stub

					parameters.put("id_mahasiswa", mahasiswaDaftarPkl.getMahasiswa().getId());
					parameters.put("id_pkl", pkl.getId());
					mahasiswaDaftarPkl.getMahasiswa().putPhoto(parameters);
					Report.generatePDFReport(Report.PDF, parameters, "kartu_daftar_pkl",
							ais.ui.util.WaktuUtil.getDate());
				}
			});
			aksiButtons.add(cetak);
			cetak.setVisible(mahasiswaDaftarPkl != null);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);
		}

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(Pkl.class)
				.add(Restrictions.or(Restrictions.isNull("jurusan"),
						Restrictions.eq("jurusan", mahasiswa == null ? null : mahasiswa.getJurusan())))
				.add(Restrictions.or(Restrictions.isNull("fakultas"),
						Restrictions.eq("fakultas", mahasiswa == null ? null : mahasiswa.getJurusan().getFakultas())))
				.add(Restrictions.or(Restrictions.isNull("program"),
						Restrictions.eq("program", mahasiswa == null ? null : mahasiswa.getProgram())));

		if (order)
			criteria.addOrder(Order.desc("tanggal_mulai")).addOrder(Order.desc("id"));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		if (grid != null) {
			Common.initPaging(initCriteria(false), paging);

			List<Pkl> pkl = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
					.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
			ListModel strset = new SimpleListModel(pkl);
			grid.setRowRenderer(new PklRenderer());
			grid.setModelCheckMobile(strset);
		}

	}

	@SuppressWarnings("unchecked")
	public boolean daftar(Pkl pkl) throws Exception {

		Session session = HibernateUtil.currentSession();
		MahasiswaDaftarPkl mahasiswaDaftarPkl = (MahasiswaDaftarPkl) session.createCriteria(MahasiswaDaftarPkl.class)
				.add(Restrictions.eq("pkl", pkl)).add(Restrictions.eq("mahasiswa", mahasiswa)).uniqueResult();

		System.out.println("mahasiswaDaftarPkl = " + mahasiswaDaftarPkl);

		if (mahasiswaDaftarPkl == null) {
			mahasiswaDaftarPkl = new MahasiswaDaftarPkl();
			mahasiswaDaftarPkl.setTanggalDaftar(ais.ui.util.WaktuUtil.getDate());
			mahasiswaDaftarPkl.setTerima(MahasiswaDaftarPkl.BELUM_DIPROSES);
		}

		mahasiswaDaftarPkl.setMemenuhiSyarat(memenuhiSyarat);
		mahasiswaDaftarPkl.setNama(mahasiswa + "-->" + pkl.getNama());
		mahasiswaDaftarPkl.setMahasiswa(mahasiswa);
		mahasiswaDaftarPkl.setPkl(pkl);

		List<MahasiswaPklPersyaratan> mahasiswaPklPersyaratans = session.createCriteria(MahasiswaPklPersyaratan.class)
				.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("pkl", pkl))
				.createAlias("persyaratanPkl", "persyaratanPkl")
				.add(Restrictions.eq("persyaratanPkl.tipeDataInputan", PersyaratanPkl.PILIHAN_CUSTOM)).list();
		Integer totalSkor = 0;
		for (MahasiswaPklPersyaratan mahasiswaPklPersyaratan : mahasiswaPklPersyaratans) {
			String val = mahasiswaPklPersyaratan.getNilaiString() == null ? ""
					: mahasiswaPklPersyaratan.getNilaiString().trim();
			String[] kol = StringUtils.split(val, ":");
			// String a = kol[0];
			Integer skor = 0;
			try {
				skor = Integer.parseInt(kol[1].trim());
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			totalSkor += skor;
		}
		mahasiswaDaftarPkl.setTotalSkor(totalSkor);

		Common.refreshSaveOrUpdate(session, mahasiswaDaftarPkl);

		MyMessageboxConfig.show("Mahasiswa dengan NIM " + mahasiswa.getNim() + " berhasil terdaftar di pkl ini",
				"Pemberitahuan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		return true;

	}

	private Boolean memenuhiSyarat = true;
	private EventListener eventListener;

	public static void onAddExternal(Event event, EventListener eventListener, Pkl pkl, Mahasiswa mahasiswa)
			throws Exception {
		PklUntukMahasiswaAction pklUntukMahasiswaAction = new PklUntukMahasiswaAction();
		pklUntukMahasiswaAction.mahasiswa = mahasiswa;
		pklUntukMahasiswaAction.eventListener = eventListener;
		pklUntukMahasiswaAction.daftar_pkl_window = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(pklUntukMahasiswaAction.daftar_pkl_window);
		pklUntukMahasiswaAction.daftar_pkl_window.setHeight("95%");
		pklUntukMahasiswaAction.daftar_pkl_window.setWidth("90%");

		pklUntukMahasiswaAction.daftar_pkl_window.setVisible(true);
		pklUntukMahasiswaAction.daftar_pkl_window.setClosable(true);
		pklUntukMahasiswaAction.daftar_pkl_window.onModal();

		pklUntukMahasiswaAction.init(pkl, false);

	}

	public static Row tampilkanPersyaratan(final PersyaratanPkl persyaratan,
			final MahasiswaPklPersyaratan temPersyaratan, Label labelLama, Rows rows, List<Component> components,
			boolean tampil, List<PersyaratanPkl> persyaratanPklsKomponen) {

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		final Label check = new Label();
		if (labelLama.getValue().isEmpty() || !labelLama.getValue().equalsIgnoreCase(persyaratan.getNama())) {
			check.setValue(persyaratan.getNama());
		}
		row.appendChild(check);

		final Component component;
		if (persyaratan.getTipeDataInputan().equals(PersyaratanPkl.TEXT)) {
			component = new Textbox(temPersyaratan == null ? null : temPersyaratan.getNilaiString());
			((Textbox) component).setWidth("90%");
			((Textbox) component).focus();
		} else if (persyaratan.getTipeDataInputan().equals(PersyaratanPkl.TANGGAL)) {
			component = new MyDatebox(temPersyaratan == null ? null : temPersyaratan.getNilaiTanggal());

			((MyDatebox) component).focus();
		} else if (persyaratan.getTipeDataInputan().equals(PersyaratanPkl.ANGKA)) {
			component = new MyDoublebox(temPersyaratan == null ? null : temPersyaratan.getNilaiNumber());

		} else if (persyaratan.getTipeDataInputan().equals(PersyaratanPkl.TEXT_ANGKA)) {
			component = new MyTextboxAngka(temPersyaratan == null ? null : temPersyaratan.getNilaiString());
			((Textbox) component).setWidth("90%");
			((Textbox) component).focus();
		} else if (persyaratan.getTipeDataInputan().equals(PersyaratanPkl.PILIHAN_YA_TIDAK)) {
			component = new Combobox();
			MyComboitemConfig comboitem = new MyComboitemConfig("Ya");
			comboitem.setValue(true);
			component.appendChild(comboitem);
			comboitem = new MyComboitemConfig("Tidak");
			comboitem.setValue(false);
			component.appendChild(comboitem);
			((Combobox) component).setReadonly(true);

			Common.selectComboItem(((Combobox) component),
					temPersyaratan == null ? null : temPersyaratan.getNilaiBoolean());
		} else if (persyaratan.getTipeDataInputan().equals(PersyaratanPkl.PILIHAN_CUSTOM)) {
			component = new Combobox();
			String[] ss = StringUtils.split(persyaratan.getNilaiDataInputan(), ";");
			Arrays.sort(ss);
			for (String s : ss) {
				// Format normal yang diharapkan: "Label:Skor" (mis. "Baik:80").
				// Sebagian data lama/tidak sesuai format hanya berisi teks/URL mentah
				// tanpa pemisah ":" atau tanpa skor numerik di bagian kedua -
				// jangan crash, tampilkan apa adanya dengan skor default 0.
				String[] kol = StringUtils.split(s, ":");
				String a;
				Integer skor = 0;
				if (kol != null && kol.length >= 2) {
					a = kol[0];
					String kolSkor = kol[1] == null ? "" : kol[1].trim();
					if (kolSkor.matches("\\d+")) {
						skor = Integer.parseInt(kolSkor);
					}
				} else {
					// tidak ada ":" (mis. URL/teks mentah) -> tampilkan seluruh string
					// sebagai label, urutan/skor default 0
					a = s;
				}
				MyComboitemConfig comboitem = new MyComboitemConfig(a);
				comboitem.setAttribute("skor", skor);
				comboitem.setValue(s);
				component.appendChild(comboitem);
			}
			((Combobox) component).setReadonly(true);

			Common.selectComboItem(((Combobox) component),
					temPersyaratan == null ? null : temPersyaratan.getNilaiString());
		} else {
			component = null;
		}

		final Label label = new Label(persyaratan.getLabelInputan() + (persyaratan.getHarusDiisi() ? " (*)" : ""));
		final Hbox hbox = new Hbox();
		hbox.setStyle("border:0px;background: transparent;");
		if (temPersyaratan != null)
			temPersyaratan.setStatus(true);
		hbox.setVisible(temPersyaratan == null ? true : temPersyaratan.getStatus());

		if (component != null) {
			persyaratanPklsKomponen.add(persyaratan);
			component.setAttribute("persyaratan", persyaratan);
			components.add(component);

			row.appendChild(label);
			label.setWidth("97%");
			if (persyaratan.getHarusDiisi()) {
				if (component instanceof Textbox) {
//					((Textbox) component).setConstraint("no empty");
				} else if (component instanceof MyDatebox) {
//					((MyDatebox) component).setConstraint("no empty");
				} else if (component instanceof MyDoublebox) {
//					((MyDoublebox) component).setConstraint("no empty");
				} else if (component instanceof Combobox) {
//					((Combobox) component).setConstraint("no empty");
				}
			}

			component.setVisible(tampil && (temPersyaratan == null ? true : temPersyaratan.getStatus()));
			label.setVisible(temPersyaratan == null ? true : temPersyaratan.getStatus());

			Vbox vbox = new Vbox();
			row.appendChild(vbox);
			vbox.appendChild(component);
			row.setValign("top");
			row.setAttribute("component", component);
			component.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (temPersyaratan != null && temPersyaratan.getId() != null) {
						if (persyaratan.getTipeDataInputan().equals(PersyaratanPkl.TEXT)) {
							temPersyaratan.setNilaiString(((Textbox) component).getValue());
						} else if (persyaratan.getTipeDataInputan().equals(PersyaratanPkl.PILIHAN_CUSTOM)) {
							temPersyaratan
									.setNilaiString((String) (((Combobox) component).getSelectedItem() == null ? ""
											: (((Combobox) component).getSelectedItem().getValue())));
						} else if (persyaratan.getTipeDataInputan().equals(PersyaratanPkl.TANGGAL)) {
							temPersyaratan.setNilaiTanggal(((MyDatebox) component).getValue());
						} else if (persyaratan.getTipeDataInputan().equals(PersyaratanPkl.ANGKA)) {
							temPersyaratan.setNilaiNumber(((MyDoublebox) component).getValue());
						} else if (persyaratan.getTipeDataInputan().equals(PersyaratanPkl.TEXT_ANGKA)) {
							temPersyaratan.setNilaiString(((MyTextboxAngka) component).getValue());
						} else if (persyaratan.getTipeDataInputan().equals(PersyaratanPkl.PILIHAN_YA_TIDAK)) {
							org.zkoss.zul.Comboitem selectedItem = ((Combobox) component).getSelectedItem();
							temPersyaratan.setNilaiBoolean(selectedItem == null ? null : (Boolean) selectedItem.getValue());
						}

						Common.refreshUpdate(temPersyaratan);
					}
				}
			});
			if (persyaratan.getHarusMenyertakanLampiran()) {
				Common.createDownloadUploadFileLampiranPkl(vbox, temPersyaratan, persyaratan.getLabelInputan());
			}
		} else {
			/*
			 * KOLOM ISIAN KOSONG. component == null bila tipeDataInputan persyaratan tidak dikenali --
			 * termasuk saat admin belum mengaturnya (default "java.lang.String" tak cocok tipe mana pun).
			 * Dulu seluruh isi baris dilewati -> kolom kanan kosong tanpa penjelasan. Kini: tombol unggah
			 * tetap tampil bila wajib lampiran, atau pesan jelas bila pengaturan tipe belum lengkap.
			 */
			final Label label2 = new Label(
					persyaratan.getLabelInputan() + (persyaratan.getHarusDiisi() ? " (*)" : ""));
			label2.setWidth("97%");
			row.appendChild(label2);

			Vbox vbox2 = new Vbox();
			row.appendChild(vbox2);
			row.setValign("top");

			if (persyaratan.getHarusMenyertakanLampiran()) {
				Common.createDownloadUploadFileLampiranPkl(vbox2, temPersyaratan, persyaratan.getLabelInputan());
			} else {
				String namaAman = persyaratan.getNama() == null ? "" : persyaratan.getNama()
						.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
				vbox2.appendChild(new ais.ui.util.MyHtml(
						"<div style='color:#b91c1c;font-size:11px;line-height:1.4;padding:5px 8px;"
								+ "border:1px solid #fecaca;border-radius:6px;background:#fef2f2;'>"
								+ "<b>Kolom isian belum dapat ditampilkan.</b><br/>"
								+ "Jenis isian untuk persyaratan \"" + namaAman + "\" belum diatur oleh admin "
								+ "(mis. berupa teks, angka, tanggal, pilihan, atau wajib melampirkan berkas). "
								+ "Mohon menghubungi admin/pengelola PKL agar melengkapi pengaturan persyaratan "
								+ "ini terlebih dahulu, sehingga mahasiswa dapat mengisinya.</div>"));
			}
		}

		if (labelLama.getValue().isEmpty() || !labelLama.getValue().equalsIgnoreCase(persyaratan.getNama())) {
			labelLama.setValue(persyaratan.getNama());
		}

		return row;
	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	private void init(final Pkl pkl, Boolean hanyaLihat) throws Exception {

		daftar_pkl_window.setTitle("Pendataan Persyaratan Pkl");
		memenuhiSyarat = true;

		Common.clear(daftar_pkl_window);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		West west = new West();
		west.setParent(borderlayout);
		west.setWidth("30%");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pendaftaran PKL"));
		row.appendChild(new ais.ui.util.MyLabelConfig(pkl.getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pendaftaran Dimulai"));
		row.appendChild(
				new Label(pkl.getTanggal_mulai() == null ? "" : Common.dateFormat4.get().format(pkl.getTanggal_mulai())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pendaftaran Ditutup"));
		row.appendChild(
				new Label(pkl.getTanggal_selesai() == null ? "" : Common.dateFormat4.get().format(pkl.getTanggal_selesai())));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM Mahasiswa"));
		row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswa.getNim()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Mahasiswa"));
		row.appendChild(new ais.ui.util.MyLabelConfig(mahasiswa.getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(
				new ais.ui.util.MyLabelConfig(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(new ais.ui.util.MyLabelConfig(
				mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getNama()));

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Borderlayout myborderlayout = new ais.ui.util.MyBorderlayout();
		myborderlayout.setParent(center);

		center = new Center();
		center.setParent(myborderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setStyle("border:0px");
		grid.setWidth("100%");
		grid.setHeight("110%");

		columns = new Columns();
		columns.setParent(grid);

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("25%");
		column.setAlign("right");

		column = new MyColumnConfig();
		column.setParent(columns);

		rows = new Rows();
		rows.setParent(grid);

		row = new MyFormRow();
		row.setStyle("border:0px;background: #8dcff4;");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Persyaratan PKL"));
		ais.ui.util.ZkCompat.setSpans(row, "3");

		row = new MyFormRow();
		row.setStyle("border:0px;background: #8dcff4;");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyHtml("<hr>"));
		ais.ui.util.ZkCompat.setSpans(row, "3");

		Session session = HibernateUtil.currentSession();
		final List<PersyaratanPkl> persyaratanPkls = session.createCriteria(PklPunyaPersyaratan.class)
				.createAlias("persyaratanPkl", "persyaratanPkl").add(Restrictions.eq("pkl", pkl))
				.add(Restrictions.or(Restrictions.eq("persyaratanPkl.jenisKelamin", ""),
						Restrictions.ilike("persyaratanPkl.jenisKelamin", mahasiswa.getKelamin(), MatchMode.ANYWHERE)))
				.setProjection(Projections.property("persyaratanPkl")).addOrder(Order.asc("persyaratanPkl.nama"))
				.addOrder(Order.asc("persyaratanPkl.labelInputan")).list();
		final List<PersyaratanPkl> persyaratanPklsKomponen = new ArrayList<PersyaratanPkl>();
		System.out.println("persyaratanPkls = " + persyaratanPkls.size());

		Label labelLama = new Label("");
		final List<Component> components = new ArrayList<Component>();
		for (final PersyaratanPkl persyaratan : persyaratanPkls) {

			MahasiswaPklPersyaratan mahasiswaPklPersyaratan = (MahasiswaPklPersyaratan) session
					.createCriteria(MahasiswaPklPersyaratan.class).add(Restrictions.eq("mahasiswa", mahasiswa))
					.add(Restrictions.eq("pkl", pkl)).add(Restrictions.eq("persyaratanPkl", persyaratan))
					.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
			if (mahasiswaPklPersyaratan == null) {
				mahasiswaPklPersyaratan = new MahasiswaPklPersyaratan();
				mahasiswaPklPersyaratan.setMahasiswa(mahasiswa);
				mahasiswaPklPersyaratan.setPkl(pkl);
				mahasiswaPklPersyaratan.setPersyaratanPkl(persyaratan);
				session.save(mahasiswaPklPersyaratan);
			}

			PklUntukMahasiswaAction.tampilkanPersyaratan(persyaratan, mahasiswaPklPersyaratan, labelLama, rows,
					components, true, persyaratanPklsKomponen);
		}

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/svg/check2-circle.svg");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				String[] nims = pkl.getNimMhsTanpaBiaya().split(",");
				boolean ada = false;
				for (String nim : nims) {
					if (mahasiswa != null && mahasiswa.getNim() != null
							&& mahasiswa.getNim().trim().equalsIgnoreCase(nim.trim())) {
						ada = true;
						break;
					}
				}

				if (!ada) {
					if (!pkl.getKodeItemBiaya().trim().isEmpty()) {

						Session session = HibernateUtil.currentSession();
						for (String kode : pkl.getKodeItemBiaya().trim().split(",")) {
							ItemBiaya itemBiaya = (ItemBiaya) session.createCriteria(ItemBiaya.class)
									.add(Restrictions.eq("kode", kode.trim())).setMaxResults(1).uniqueResult();
							if (itemBiaya != null) {
								int jumlah = ((Number) session.createCriteria(CicilanPembayaran.class)
										.createAlias("kegiatan", "kegiatan")
										.add(Restrictions.eq("itemBiaya", itemBiaya))
										.add(Restrictions.eq("kegiatan.mahasiswa", mahasiswa))
										.setProjection(Projections.rowCount()).uniqueResult()).intValue();
								if (jumlah == 0) {
									MyMessageboxConfig.show(
											"Mahasiswa dengan NIM " + mahasiswa.getNim() + " dan nama "
													+ mahasiswa.getNama() + " belum membayar biaya "
													+ itemBiaya.getKode() + "-" + itemBiaya.getNama()
													+ ". Harap menghubungi bagian keuangan untuk melakukan pembayaran.",
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
									daftar_pkl_window.setVisible(false);
									return;
								}
							}
						}

					}

					if (pkl.getHarusBayar()) {

						Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(), pkl.getTahunAkademik(),
								pkl.getSemester(), mahasiswa.getPindahKeKampusIniMasukSemester(),
								mahasiswa.getSemesterMulai());

						if (!Common.checkStatusPembayaranMahasiswa(smt, mahasiswa.currentTahapan(), mahasiswa, false,
								false)) {
							MyMessageboxConfig.show("Mahasiswa dengan NIM " + mahasiswa.getNim()
									+ " tidak diperkenankan mendaftar, karena belum membayar biaya perkuliahan semester "
									+ smt
									+ (mahasiswa.currentTahapan() != null && mahasiswa.currentTahapan() > 0
											? " tahap " + mahasiswa.currentTahapan()
											: "")
									+ ". Langkah yang dapat dilakukan: (1) Hubungi bagian keuangan untuk melakukan pembayaran biaya perkuliahan; (2) Setelah pembayaran dikonfirmasi oleh sistem, coba daftarkan kembali; (3) Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
									"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
							daftar_pkl_window.setVisible(false);
							return;
						}
					}
				}
				if (!Common.checkSyaratPkl(mahasiswa, pkl)) {
					return;
				}
				if (Common.bolehKonfigurasi("jika_sudah_dapat_pkl_mahasiswa_tidak_boleh_mengajukan_pkl")) {
					int jumlah = ((Number) HibernateUtil.currentSession().createCriteria(MahasiswaDaftarPkl.class)
							.add(Restrictions.eq("mahasiswa", mahasiswa))
							.add(pkl.getId() != null ? Restrictions.ne("pkl", pkl)
									: Restrictions.sqlRestriction("true"))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();
					if (jumlah > 0) {
						MyMessageboxConfig.show("Mahasiswa dengan NIM " + mahasiswa.getNim()
								+ " tidak diperkenankan mendaftar PKL ini, karena sudah pernah mendaftarkan PKL di tempat lain. Langkah yang dapat dilakukan: (1) Periksa apakah mahasiswa sudah terdaftar di PKL lain; (2) Hubungi bagian akademik atau kemahasiswaan jika ingin mengubah pendaftaran PKL; (3) Jika masih mengalami kendala, hubungi Administrator atau tim teknis.",
								"INFORMATION", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
				}

				if (!memenuhiSyarat) {
					MyMessageboxConfig.show("Mohon maaf, data pendaftaran PKL belum memenuhi syarat. Langkah yang dapat dilakukan: (1) Periksa kembali seluruh persyaratan PKL yang wajib dipenuhi; (2) Pastikan semua isian dan lampiran sudah dilengkapi dengan benar; (3) Ulangi proses pendaftaran. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				for (final Component component : components) {

					if (component.getAttribute("persyaratan") != null) {
						PersyaratanPkl persyaratan = (PersyaratanPkl) component.getAttribute("persyaratan");
						if (!persyaratan.getHarusDiisi()) {
							continue;
						}
					}

					if (component instanceof Textbox && ((Textbox) component).getValue().trim().isEmpty()) {
						MyMessageboxConfig.show("Mohon maaf, masih terdapat persyaratan PKL yang belum diisi. Langkah yang dapat dilakukan: (1) Isi semua kolom persyaratan yang diwajibkan (ditandai tanda bintang); (2) Pastikan tidak ada kolom yang masih kosong; (3) Klik Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										((Textbox) component).focus();
									}
								});
						return;
					} else if (component instanceof MyDatebox && ((MyDatebox) component).getValue() == null) {
						MyMessageboxConfig.show("Mohon maaf, masih terdapat persyaratan PKL yang belum diisi. Langkah yang dapat dilakukan: (1) Isi semua kolom persyaratan yang diwajibkan (ditandai tanda bintang); (2) Pastikan tidak ada kolom yang masih kosong; (3) Klik Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										((MyDatebox) component).focus();
									}
								});
						return;
					} else if (component instanceof MyDoublebox && ((MyDoublebox) component).getValue() == null) {
						MyMessageboxConfig.show("Mohon maaf, masih terdapat persyaratan PKL yang belum diisi. Langkah yang dapat dilakukan: (1) Isi semua kolom persyaratan yang diwajibkan (ditandai tanda bintang); (2) Pastikan tidak ada kolom yang masih kosong; (3) Klik Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										((MyDoublebox) component).focus();
									}
								});
						return;
					} else if (component instanceof Combobox && ((Combobox) component).getSelectedItem() == null) {
						MyMessageboxConfig.show("Mohon maaf, masih terdapat persyaratan PKL yang belum diisi. Langkah yang dapat dilakukan: (1) Isi semua kolom persyaratan yang diwajibkan (ditandai tanda bintang); (2) Pastikan tidak ada kolom yang masih kosong; (3) Klik Simpan kembali. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										((Combobox) component).focus();
									}
								});
						return;
					}
				}

//				Session session = HibernateUtil.currentSession();
//				for (final PersyaratanPkl persyaratan : persyaratanPklsKomponen) {
//					if (persyaratan.getHarusMenyertakanLampiran()) {
//						MahasiswaPklPersyaratan mahasiswaPklPersyaratan = (MahasiswaPklPersyaratan) session
//								.createCriteria(MahasiswaPklPersyaratan.class)
//								.add(Restrictions.eq("mahasiswa", mahasiswa)).add(Restrictions.eq("pkl", pkl))
//								.addOrder(Order.desc("id")).setMaxResults(1)
//								.add(Restrictions.eq("persyaratanPkl", persyaratan)).uniqueResult();
//						if (mahasiswaPklPersyaratan == null) {
//							mahasiswaPklPersyaratan = new MahasiswaPklPersyaratan();
//							mahasiswaPklPersyaratan.setMahasiswa(mahasiswa);
//							mahasiswaPklPersyaratan.setPkl(pkl);
//							mahasiswaPklPersyaratan.setPersyaratanPkl(persyaratan);
//							session.save(mahasiswaPklPersyaratan);
//						}
//
//						FileFotoLain fileFotoLain = FileFotoLain.ambil(mahasiswaPklPersyaratan.getId(),
//								LampiranPklMahasiswa.DEFAULT_JENIS, LampiranPklMahasiswa.class, true);
//						if (fileFotoLain == null) {
//							MyMessageboxConfig.show("\"" + persyaratan.getNama() + "\" harus di upload", "Informasi",
//									MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
//							return;
//						}
//
//					}
//				}

				if (daftar(pkl)) {
					onSearchDefault(null);
					daftar_pkl_window.setVisible(false);

					if (eventListener != null) {
						eventListener.onEvent(null);
					}
				}
			}
		});
		save.setParent(toolbar);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				daftar_pkl_window.setVisible(false);
			}
		});
		cancel.setParent(toolbar);

		borderlayout.setParent(daftar_pkl_window);
		daftar_pkl_window.setVisible(true);
		daftar_pkl_window.onModal();

		if (hanyaLihat) {
			Common.freeze(daftar_pkl_window, true);
			save.setVisible(false);
			cancel.setDisabled(false);
		}

	}
}
