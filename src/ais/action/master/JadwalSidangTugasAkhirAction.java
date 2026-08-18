package ais.action.master;


import ais.common.CommonSearchFilterHelper;
import java.util.Date;
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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataRuangBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.format1.akademik.LaporanRekapitulasiSidang;
import ais.action.report.format1.akademik.LaporanSidang;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.JadwalSidangTugasAkhir;
import ais.database.model.Jurusan;
import ais.database.model.Ruang;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class JadwalSidangTugasAkhirAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchketerangan;
	private Combobox searchtahunakademik;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchprogram;

	private Textbox nama;
	private MyDatebox mulai;
	private MyDatebox sampai;
	private Combobox tahunAkademik;
	private Combobox jurusan;
	private Combobox fakultas;
	private Textbox keterangan;
	private AmbilDataRuangBanbox ruangSidang;

	private boolean edit = false;
	private boolean delete = false;

	private JadwalSidangTugasAkhir jadwalSidangTugasAkhir;
	private MyToolbarbuttonConfig add;
	private EventListener eventListener;
	private Tbmuser tbmuser;
	private Combobox program;
	private Rows subrows;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();

		tbmuser = Common.getCurrentUser();

		Common.generateTahunAjaranDanSemua(searchtahunakademik);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE) && tbmuser.getMahasiswa() == null
				&& tbmuser.ambilDosen() == null);
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		Common.initPrograms(searchprogram);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	public static void onAddExternal(Event event, EventListener eventListener,
			JadwalSidangTugasAkhir jadwalSidangTugasAkhir) throws Exception {
		JadwalSidangTugasAkhirAction jadwalSidangTugasAkhirAction = new JadwalSidangTugasAkhirAction();
		jadwalSidangTugasAkhirAction.eventListener = eventListener;
		jadwalSidangTugasAkhirAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot()
				.appendChild(jadwalSidangTugasAkhirAction.addWindow);
		jadwalSidangTugasAkhirAction.addWindow.setHeight("350px");
		jadwalSidangTugasAkhirAction.addWindow.setWidth("550px");

		jadwalSidangTugasAkhirAction.init(jadwalSidangTugasAkhir);

		jadwalSidangTugasAkhirAction.addWindow.setVisible(true);
		jadwalSidangTugasAkhirAction.addWindow.onModal();
	}

	class JadwalSidangTugasAkhirRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {arg0.setValign("top");
			// TODO Auto-generated method stub
			final JadwalSidangTugasAkhir jadwalSidangTugasAkhir = (JadwalSidangTugasAkhir) arg1;

			MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.setOpen(true);
			detail.appendChild(initJadwal(jadwalSidangTugasAkhir));

			RevisiHelper.createNewRevisi(JadwalSidangTugasAkhir.class, jadwalSidangTugasAkhir,
					jadwalSidangTugasAkhir.getNama()).setParent(arg0);

			new Label(jadwalSidangTugasAkhir.getMulai() == null ? ""
					: Common.dateFormat.get().format(jadwalSidangTugasAkhir.getMulai())).setParent(arg0);
			new Label(jadwalSidangTugasAkhir.getSampai() == null ? ""
					: Common.dateFormat.get().format(jadwalSidangTugasAkhir.getSampai())).setParent(arg0);
			new Label(jadwalSidangTugasAkhir.getTahunAkademik()).setParent(arg0);
			new Label(jadwalSidangTugasAkhir.getRuangSidang() == null ? ""
					: jadwalSidangTugasAkhir.getRuangSidang().getNama()).setParent(arg0);
			new Label(jadwalSidangTugasAkhir.getProgram() == null ? "Semua" : jadwalSidangTugasAkhir.getProgram())
					.setParent(arg0);
			new Label(jadwalSidangTugasAkhir.getJurusan() == null ? "Semua"
					: jadwalSidangTugasAkhir.getJurusan().getNama()).setParent(arg0);
			new Label(jadwalSidangTugasAkhir.getFakultas() == null ? "Semua"
					: jadwalSidangTugasAkhir.getFakultas().getNama()).setParent(arg0);
			new Label(jadwalSidangTugasAkhir.getKeterangan()).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			Hbox toolbar = new Hbox();
			toolbar.setParent(vbox);
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					LaporanSidang laporanRekapitulasiSidang = new LaporanSidang(jadwalSidangTugasAkhir);
					laporanRekapitulasiSidang.setClosable(true);
					laporanRekapitulasiSidang.setTitle("Laporan Sidang");
					laporanRekapitulasiSidang.setHeight("97%");
					laporanRekapitulasiSidang.setWidth("97%");
					laporanRekapitulasiSidang.setParent(page.getFirstRoot());
					laporanRekapitulasiSidang.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Rekap", "/img/print.png");
			button.setTooltiptext("Cetak");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					LaporanRekapitulasiSidang laporanRekapitulasiSidang = new LaporanRekapitulasiSidang(
							jadwalSidangTugasAkhir);
					laporanRekapitulasiSidang.setClosable(true);
					laporanRekapitulasiSidang.setTitle("Laporan Jadwal Sidang");
					laporanRekapitulasiSidang.setHeight("97%");
					laporanRekapitulasiSidang.setWidth("97%");
					laporanRekapitulasiSidang.setParent(page.getFirstRoot());
					laporanRekapitulasiSidang.onModal();
				}

			});
			button.setParent(toolbar);

			toolbar = new Hbox();
			toolbar.setParent(vbox);

			button = new MyToolbarbuttonConfig("Ubah", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit && tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(jadwalSidangTugasAkhir);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(
					delete && tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
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
											onDelete(jadwalSidangTugasAkhir);
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig
													.show("Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			button.setParent(toolbar);

		}

	}

	public static void onDelete(JadwalSidangTugasAkhir jadwalSidangTugasAkhir) {

		Common.refreshDelete(jadwalSidangTugasAkhir);
	}

	public void onAdd(Event event) throws Exception {
		init(new JadwalSidangTugasAkhir());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private Grid initJadwal(final JadwalSidangTugasAkhir jadwalSidangTugasAkhir) {

		Grid subGrid = new Grid();

		Columns subcolumns = new Columns();
		subcolumns.setParent(subGrid);

		MyColumnConfig subcolumn = new MyColumnConfig("Nama Acara / Jadwal");
		subcolumn.setParent(subcolumns);
		subcolumn.setWidth("26%");

		subcolumn = new MyColumnConfig("Waktu dan Tanggal Mulai");
		subcolumn.setParent(subcolumns);
		subcolumn.setWidth("20%");

		subcolumn = new MyColumnConfig("Waktu dan Tanggal Sampai");
		subcolumn.setParent(subcolumns);
		subcolumn.setWidth("20%");

		subcolumn = new MyColumnConfig("Keterangan");
		subcolumn.setParent(subcolumns);
		subcolumn.setWidth("26%");

		subcolumn = new MyColumnConfig("Hapus");
		subcolumn.setParent(subcolumns);
		subcolumn.setWidth("8%");

		final Rows subrows = new Rows();
		subrows.setParent(subGrid);

		List<Object[]> list = jadwalSidangTugasAkhir.daftarJadwal();

		for (Object[] o : list) {

			try {
				final String n = o[0].toString();
				final MyFormRow subrow = new MyFormRow();
				subrow.setParent(subrows);
				subrow.setValign("top");subrow.setAttribute("o", o);

				subrow.appendChild(new Label(n));
				subrow.appendChild(new Label(formatTanggalJadwal(o[1])));
				subrow.appendChild(new Label(formatTanggalJadwal(o[2])));
				subrow.appendChild(new Label(o[3].toString()));

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.setVisible(
						delete && tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
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
											if (jadwalSidangTugasAkhir.getId() != null) {
												jadwalSidangTugasAkhir.hapusJadwal(n);
												Common.refreshUpdate(jadwalSidangTugasAkhir);
											}
											subrow.detach();
										}

									}
								});

					}
				});
				button.setParent(subrow);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}
		}

		MyFormRow subrow = new MyFormRow();
		subrow.setParent(subrows);
		ais.ui.util.ZkCompat.setSpans(subrow, "5");
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Tambah Agenda / Jadwal", "/img/add_item.png");
		button.setTooltiptext("Hapus Data");
		button.setVisible(delete && tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				final MyFormRow subrow = new MyFormRow();
				subrow.setParent(subrows);

				final Textbox nama = new Textbox();
				final MyDatebox mulai = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
				mulai.setFormat(Common.dateFormat.get().toPattern());
				final MyDatebox sampai = new MyDatebox(ais.ui.util.WaktuUtil.getDate());
				sampai.setFormat(Common.dateFormat.get().toPattern());
				final Textbox keterangan = new Textbox();

				if (jadwalSidangTugasAkhir.getId() != null) {
					EventListener eventListener = new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							jadwalSidangTugasAkhir.populateJadwal(nama.getValue(), mulai.getValue(), sampai.getValue(),
									keterangan.getValue());
							Common.refreshUpdate(jadwalSidangTugasAkhir);
						}
					};
					nama.addEventListener("onChange", eventListener);
					mulai.addEventListener("onChange", eventListener);
					sampai.addEventListener("onChange", eventListener);
					keterangan.addEventListener("onChange", eventListener);
				}

				subrow.setValign("top");subrow.setAttribute("nama", nama);
				subrow.setValign("top");subrow.setAttribute("mulai", mulai);
				subrow.setValign("top");subrow.setAttribute("sampai", sampai);
				subrow.setValign("top");subrow.setAttribute("keterangan", keterangan);

				nama.setWidth("90%");
				nama.setRows(2);
				keterangan.setWidth("90%");
				keterangan.setRows(2);
				mulai.setWidth("90%");
				sampai.setWidth("90%");

				subrow.appendChild(nama);
				subrow.appendChild(mulai);
				subrow.appendChild(sampai);
				subrow.appendChild(keterangan);

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.setVisible(
						delete && tbmuser != null && tbmuser.getMahasiswa() == null &&  tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null);
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
											if (jadwalSidangTugasAkhir.getId() != null) {
												jadwalSidangTugasAkhir.hapusJadwal(nama.getValue());
												Common.refreshUpdate(jadwalSidangTugasAkhir);
											}
											subrow.detach();
										}

									}
								});

					}
				});
				button.setParent(subrow);
			}
		});
		button.setParent(subrow);
		return subGrid;
	}

	private String formatTanggalJadwal(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof java.util.Date) {
			return Common.dateFormat51.get().format((java.util.Date) value);
		}
		return value.toString();
	}

	@SuppressWarnings("deprecation")
	private void init(JadwalSidangTugasAkhir jadwalSidangTugasAkhir) {
		final Tbmuser tbmuser = Common.getCurrentUser();
		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusanDanSemua(fakultas, jurusan, null, null);

		this.jadwalSidangTugasAkhir = jadwalSidangTugasAkhir;
		addWindow.setTitle(jadwalSidangTugasAkhir.getId() == null ? "Tambah Jadwal " : "Ubah Jadwal ");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama Jadwal"));
		row.appendChild(
				nama = new Textbox(jadwalSidangTugasAkhir.getNama() == null ? "" : jadwalSidangTugasAkhir.getNama()));
		nama.setWidth("90%");
		nama.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai"));
		row.appendChild(mulai = new MyDatebox(jadwalSidangTugasAkhir.getMulai()));
		mulai.setWidth("90%");
		mulai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sampai"));
		row.appendChild(sampai = new MyDatebox(jadwalSidangTugasAkhir.getSampai()));
		sampai.setWidth("90%");
		sampai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ruang "));
		row.appendChild(ruangSidang = new AmbilDataRuangBanbox());
		ruangSidang.setValue(jadwalSidangTugasAkhir == null || jadwalSidangTugasAkhir.getRuangSidang() == null ? ""
				: jadwalSidangTugasAkhir.getRuangSidang().getKodeRuangan() + " - "
						+ jadwalSidangTugasAkhir.getRuangSidang().getNama());
		ruangSidang.setAttribute("ruang", jadwalSidangTugasAkhir.getRuangSidang());
		ruangSidang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunAkademik = new Combobox());
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		tahunAkademik.setWidth("90%");
		tahunAkademik.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		Common.selectComboItem(fakultas, jadwalSidangTugasAkhir.getFakultas() == null ? tbmuser.ambilFakultas()
				: jadwalSidangTugasAkhir.getFakultas());
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan, jadwalSidangTugasAkhir.getJurusan() == null ? tbmuser.ambilJurusan()
				: jadwalSidangTugasAkhir.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		program = Common.initPrograms(program);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(program);
		Common.selectComboItem(program, jadwalSidangTugasAkhir.getProgram());
		program.setWidth("90%");
		program.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				jadwalSidangTugasAkhir.getKeterangan() == null ? "" : jadwalSidangTugasAkhir.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		Grid subGrid = initJadwal(jadwalSidangTugasAkhir);
		row.appendChild(subGrid);
		subrows = subGrid.getRows();

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {

					if (eventListener != null) {
						eventListener.onEvent(
								new Event("", addWindow, JadwalSidangTugasAkhirAction.this.jadwalSidangTugasAkhir));
					}

					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (ruangSidang.getAttribute("ruang") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Ruang sidang",
					"Kolom Ruang sidang belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Ruang sidang.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tahunAkademik.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun Akademik",
					"Kolom Tahun Akademik belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tahun Akademik.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jadwalSidangTugasAkhir.getId() != null) {
			jadwalSidangTugasAkhir = (JadwalSidangTugasAkhir) session.load(JadwalSidangTugasAkhir.class,
					jadwalSidangTugasAkhir.getId());

		}
		jadwalSidangTugasAkhir
				.setProgram((String) (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null
						? null : program.getSelectedItem().getValue()));
		jadwalSidangTugasAkhir.setRuangSidang((Ruang) ruangSidang.getAttribute("ruang"));
		jadwalSidangTugasAkhir.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		jadwalSidangTugasAkhir.setNama(nama.getValue());
		jadwalSidangTugasAkhir.setKeterangan(keterangan.getValue());
		jadwalSidangTugasAkhir
				.setJurusan((Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null
						? null : jurusan.getSelectedItem().getValue()));
		jadwalSidangTugasAkhir.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));
		jadwalSidangTugasAkhir.setMulai(mulai.getValue());
		jadwalSidangTugasAkhir.setSampai(sampai.getValue());

		if (subrows != null) {
			jadwalSidangTugasAkhir.setJadwalRinci("");
			@SuppressWarnings("unchecked")
			List<Row> rows = subrows.getChildren();
			for (Row row : rows) {
				if (row.getAttribute("nama") != null) {
					Textbox nama = (Textbox) row.getAttribute("nama");
					MyDatebox mulai = (MyDatebox) row.getAttribute("mulai");
					MyDatebox sampai = (MyDatebox) row.getAttribute("sampai");
					Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null && row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan") : null);

					jadwalSidangTugasAkhir.populateJadwal(nama.getValue(), mulai.getValue(), sampai.getValue(),
							keterangan.getValue());
				} else if (row.getAttribute("o") != null) {
					Object[] o = (Object[]) row.getAttribute("o");
					jadwalSidangTugasAkhir.populateJadwal((String) o[0], (Date) o[1], (Date) o[2], (String) o[3]);
				}
			}
		}

		Common.refreshSaveOrUpdate(session, jadwalSidangTugasAkhir);

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JadwalSidangTugasAkhir.class);
		if (order)
			criteria.addOrder(Order.asc("mulai"));
		if (order)
			criteria.addOrder(Order.asc("sampai"));

		criteria.add(Restrictions.ilike("keterangan", searchketerangan.getValue(), MatchMode.ANYWHERE))
				.add(searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE))
				.add(searchtahunakademik.getSelectedItem() == null
						|| searchtahunakademik.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchtahunakademik.getSelectedItem().getValue()))

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchketerangan == null) {
			return;
		}
		Common.initPaging(initCriteria(false), paging);
		List<JadwalSidangTugasAkhir> jadwalSidangTugasAkhir = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jadwalSidangTugasAkhir);
		grid.setRowRenderer(new JadwalSidangTugasAkhirRenderer());
		grid.setModelCheckMobile(strset);

	}
}
