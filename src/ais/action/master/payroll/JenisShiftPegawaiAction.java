package ais.action.master.payroll;


import ais.common.CommonSearchFilterHelper;
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
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.East;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabbox;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Tabpanels;
import org.zkoss.zul.Tabs;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.payroll.detail.JenisShiftPunyaPegawaiAction;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Tbmuser;
import ais.database.model.asset.Lokasi;
import ais.database.model.payroll.DetailJenisShiftPegawai;
import ais.database.model.payroll.JenisShiftPegawai;
import ais.database.model.payroll.WaktuShift;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataInitDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyIntbox;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTabConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyTimebox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.action.master.helper.FilterLanjutHelper;

public class JenisShiftPegawaiAction extends GenericAutowireComposer implements DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private Window addWindow;
	private MyGrid grid;
	private Paging paging;

	private MyTextbox searchnama;
	private Row hbFakultasLabel;
	private Row hbYayasan;
	private boolean pt = false;
	private boolean ya = false;

	private Combobox searchfakultas;
	private Combobox searchjurusan;
	private Combobox searchyayasan;
	private Combobox searchsekolah;

	private MyTextbox nama;
	private Intbox jumlahShift;
	private MyDatebox berlakuMulai;
	private MyDatebox berlakuSampai;
	private MyTextbox keterangan;

	private Checkbox searchaktif;

	private boolean edit = false;
	private boolean delete = false;

	private JenisShiftPegawai jenisShiftPegawai;
	private MyToolbarbuttonConfig add;
//	private Rows rowsShift;
	private Combobox lokasi;
	private MyDoublebox jarak;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox yayasan;
	private Combobox sekolah;
	private Combobox lokasi2;
	private Combobox lokasi3;
	private Combobox lokasi4;
	private Combobox lokasi5;
	private Tabs tabs;
	private MyCheckboxConfig berotasi;
	private MyCheckboxConfig defaultAbsenGuru;
	private MyCheckboxConfig defaultAbsenDosen;
	private MyCheckboxConfig defaultAbsenPegawai;
	private MyCheckboxConfig defaultMahasiswa;
	private MyCheckboxConfig defaultSiswa;
	private MyCheckboxConfig hariLiburDitentukan;
	private MyCheckboxConfig jumlahHariSamaDenganJumlahShift;
	private MyCheckboxConfig harusMengikutiStateMasukDanPulang;
	private MyIntbox jumlahHari;
	private MyDoublebox waktuBekerjaMinimal;
	private Combobox lokasi6;
	private Combobox lokasi7;
	private Combobox lokasi8;
	private Combobox lokasi9;
	private Combobox lokasi10;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah, true, false);

		if (hbFakultasLabel != null) { hbFakultasLabel.setVisible(pt && searchfakultas.getChildren().size() > 1); }
		if (hbYayasan != null) { hbYayasan.setVisible(ya); }

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

	        FilterLanjutHelper.setup(comp);
}

	class JenisShiftPegawaiRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final JenisShiftPegawai jenisShiftPegawai = (JenisShiftPegawai) arg1;

			new JenisShiftPunyaPegawaiAction(jenisShiftPegawai).setParent(arg0);

			RevisiHelper.createNewRevisi(JenisShiftPegawai.class, jenisShiftPegawai, jenisShiftPegawai.getNama())
					.setParent(arg0);
			new Label(Common.numberFormat.get().format(jenisShiftPegawai.getJumlahShift())).setParent(arg0);
			new Label(Common.numberFormat.get().format(jenisShiftPegawai.getJumlahHari())).setParent(arg0);

			new Label(

					(jenisShiftPegawai.getBerlakuMulai() == null ? ""
							: Common.dateFormat4.get().format(jenisShiftPegawai.getBerlakuMulai())) + " "
							+ (jenisShiftPegawai.getBerlakuSampai() == null ? ""
									: " sd " + Common.dateFormat4.get().format(jenisShiftPegawai.getBerlakuSampai()))

			).setParent(arg0);

			Vbox vbox = new Vbox();
			vbox.setParent(arg0);

			new MyLabelAgakKecil(
					jenisShiftPegawai.getLokasi() == null ? "Semua Lokasi" : jenisShiftPegawai.getLokasi().getNama())
					.setParent(vbox);

			if (jenisShiftPegawai.getLokasi2() != null) {
				new MyLabelAgakKecil(jenisShiftPegawai.getLokasi2().getNama()).setParent(vbox);
			}
			if (jenisShiftPegawai.getLokasi3() != null) {
				new MyLabelAgakKecil(jenisShiftPegawai.getLokasi3().getNama()).setParent(vbox);
			}
			if (jenisShiftPegawai.getLokasi4() != null) {
				new MyLabelAgakKecil(jenisShiftPegawai.getLokasi4().getNama()).setParent(vbox);
			}
			if (jenisShiftPegawai.getLokasi5() != null) {
				new MyLabelAgakKecil(jenisShiftPegawai.getLokasi5().getNama()).setParent(vbox);
			}
			if (jenisShiftPegawai.getLokasi6() != null) {
				new MyLabelAgakKecil(jenisShiftPegawai.getLokasi6().getNama()).setParent(vbox);
			}
			if (jenisShiftPegawai.getLokasi7() != null) {
				new MyLabelAgakKecil(jenisShiftPegawai.getLokasi7().getNama()).setParent(vbox);
			}
			if (jenisShiftPegawai.getLokasi8() != null) {
				new MyLabelAgakKecil(jenisShiftPegawai.getLokasi8().getNama()).setParent(vbox);
			}
			if (jenisShiftPegawai.getLokasi9() != null) {
				new MyLabelAgakKecil(jenisShiftPegawai.getLokasi9().getNama()).setParent(vbox);
			}
			if (jenisShiftPegawai.getLokasi10() != null) {
				new MyLabelAgakKecil(jenisShiftPegawai.getLokasi10().getNama()).setParent(vbox);
			}

			new Label(Common.numberFormat.get().format(jenisShiftPegawai.getJarak()) + " km").setParent(arg0);

			new Label(jenisShiftPegawai.getKeterangan()).setParent(arg0);

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Aktif");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(jenisShiftPegawai.getAktif());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					jenisShiftPegawai.setAktif(checkbox.isChecked());
					Common.refreshSaveOrUpdate(jenisShiftPegawai);
				}
			});

			Common.copyEditDeleteButtons(edit, delete, jenisShiftPegawai, JenisShiftPegawaiAction.this).setParent(arg0);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new JenisShiftPegawai());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		JenisShiftPegawai jenisShiftPegawai = (JenisShiftPegawai) obj;
		init(jenisShiftPegawai);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@SuppressWarnings("deprecation")
	private void initShiftDetail(East east, final JenisShiftPegawai jenisShiftPegawai) throws Exception {
		Common.clear(east);
		ais.ui.util.ZkCompat.setFlex(east, true);
		Tabbox tabbox = new Tabbox();
		tabbox.setParent(Common.tampilanScroll1(east));
		tabbox.setHeight("100%");
		tabbox.setWidth("100%");
		tabbox.setOrient("vertical");
		tabbox.setHeight("2000px");

		tabs = new Tabs();
		tabs.setWidth("30px");
		tabs.setParent(tabbox);

		Tabpanels tabpanels = new Tabpanels();
		tabpanels.setParent(tabbox);

		Session session = HibernateUtil.currentSession();

		for (int i = 1; i <= jenisShiftPegawai.getJumlahShift(); i++) {

			DetailJenisShiftPegawai detailJenisShiftPegawaiTemp = jenisShiftPegawai == null
					|| jenisShiftPegawai.getId() == null
							? new DetailJenisShiftPegawai()
							: (DetailJenisShiftPegawai) ConstantValues.simpleObject(session
									.createCriteria(DetailJenisShiftPegawai.class).add(Restrictions.eq("ke", i))
									.add(Restrictions.eq("jenisShiftPegawai", jenisShiftPegawai)).setMaxResults(1),
									DetailJenisShiftPegawai.class);
			if (detailJenisShiftPegawaiTemp == null) {
				detailJenisShiftPegawaiTemp = new DetailJenisShiftPegawai();
			}
			final DetailJenisShiftPegawai detailJenisShiftPegawai = detailJenisShiftPegawaiTemp;
			detailJenisShiftPegawai.setJenisShiftPegawai(jenisShiftPegawai);
			detailJenisShiftPegawai.setKe(i);

			final MyTabConfig tabSoal = new MyTabConfig(detailJenisShiftPegawai.getNama());
			tabSoal.setStyle("writing-mode: vertical-rl;text-orientation: mixed;");
			tabSoal.setAttribute("detailJenisShiftPegawai", detailJenisShiftPegawai);
			tabSoal.setParent(tabs);

			final Tabpanel tabpanel = new ais.ui.util.MyTabpanel();
			tabpanel.setParent(tabpanels);

			MyGroupboxStyled groupboxStyled = new MyGroupboxStyled();
			groupboxStyled.setParent(tabpanel);
			groupboxStyled.setWidth("90%");
			groupboxStyled.appendChild(new MyCaptionStyled("Shift ke-" + detailJenisShiftPegawai.getKe()));

			Vbox vbox = new Vbox();
			vbox.setWidth("90%");
			vbox.setParent(groupboxStyled);

			vbox.appendChild(new MyLabelStyled("Rincian Waktu Kerja"));

			Grid ket = new Grid();
			ket.setSclass("dgrid");
			ket.setParent(vbox);
			ket.setWidth("100%");
			ket.setHeight("100%");

			Columns columnsKet = new Columns();
			columnsKet.setParent(ket);

			Column columnKet = new Column("");
			columnsKet.appendChild(columnKet);
			columnKet = new Column("");
			columnsKet.appendChild(columnKet);
			columnsKet.setWidth("70%");

			Rows rowsKet = new Rows();
			rowsKet.setParent(ket);

			MyFormRow rowKet = new MyFormRow();
			rowKet.setVisible(jenisShiftPegawai.getHariLiburDitentukan());
			rowKet.setParent(rowsKet);
//			rowKet.appendChild(new MyLabelConfig("Apakah shift ini libur ?"));

			RevisiHelper
					.createNewRevisi(DetailJenisShiftPegawai.class, detailJenisShiftPegawai, "Apakah shift ini libur ?")
					.setParent(rowKet);

			final MyCheckboxConfig khususBuatHariLibur;
			rowKet.appendChild(khususBuatHariLibur = new MyCheckboxConfig("Ya, hari libur"));
			khususBuatHariLibur.setChecked(detailJenisShiftPegawai.getKhususBuatHariLibur());
			khususBuatHariLibur.setAttribute("janganDisabled", true);

			EventListener eventListenerLibur = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.freeze(tabpanel, khususBuatHariLibur.isChecked());
						}
					});
				}
			};

			khususBuatHariLibur.addEventListener("onClick", eventListenerLibur);

			rowKet = new MyFormRow();
			rowKet.setVisible(!jenisShiftPegawai.getBerotasi());
			rowKet.setParent(rowsKet);
			try {
				RevisiHelper.createNewRevisi(DetailJenisShiftPegawai.class, detailJenisShiftPegawai, "Hari:")
						.setParent(rowKet);
			} catch (Exception e) {
				rowKet.appendChild(new MyLabelConfig("Hari:"));
			}

			final Combobox hari = Common.createComboHariDanSemua();
			rowKet.appendChild(hari);
			hari.setWidth("90%");
			Common.selectComboItem(hari, detailJenisShiftPegawai.getHari());

			final Combobox hariKe = new Combobox();
			for (int h = 1; h <= jenisShiftPegawai.getJumlahHari(); h++) {
				Comboitem comboitem = new Comboitem();
				comboitem.setLabel("ke-" + h);
				comboitem.setValue(h);
				hariKe.appendChild(comboitem);
			}
			rowKet.appendChild(hariKe);
			hariKe.setWidth("90%");
			hariKe.setReadonly(true);
			Common.selectComboItem(hariKe, detailJenisShiftPegawai.getHariKe());

			rowKet = new MyFormRow();
			rowKet.setVisible(!jenisShiftPegawai.getJumlahHariSamaDenganJumlahShift());
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Hari ke:"));
			rowKet.appendChild(hariKe);

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Waktu Shift:"));

			final Combobox waktuShift = new Combobox();
			Common.insertComboDanSemua(waktuShift, new String[] { "nama" }, "keterangan", WaktuShift.class,
					"Waktu Shift Kustom", Restrictions.eq("aktif", true));
			rowKet.appendChild(waktuShift);
			waktuShift.setWidth("90%");
			Common.selectComboItem(true, waktuShift, detailJenisShiftPegawai.getWaktuShift());

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Mulai:"));
			final Timebox mulai;
			rowKet.appendChild(mulai = new MyTimebox(detailJenisShiftPegawai.getMulai()));
			mulai.setWidth("90%");

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Sampai:"));
			final Timebox sampai;
			rowKet.appendChild(sampai = new MyTimebox(detailJenisShiftPegawai.getSampai()));
			sampai.setWidth("90%");

			EventListener eventListenerwaktuShift = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					WaktuShift w = (WaktuShift) (waktuShift.getSelectedItem() == null ? null
							: waktuShift.getSelectedItem().getValue());
					mulai.setDisabled(w != null);
					sampai.setDisabled(w != null);

					if (w != null) {
						mulai.setValue(w.getMulai());
						sampai.setValue(w.getSampai());
					}
				}
			};

			eventListenerwaktuShift.onEvent(null);
			waktuShift.addEventListener("onChange", eventListenerwaktuShift);

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Mulai Lembur:"));
			final Timebox lemburMulai;
			rowKet.appendChild(lemburMulai = new MyTimebox(detailJenisShiftPegawai.getLemburMulai()));
			lemburMulai.setWidth("90%");

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Maks Lembur:"));
			final MyDoublebox lemburMaks;
			rowKet.appendChild(lemburMaks = new MyDoublebox(detailJenisShiftPegawai.getLemburMaks()));
			lemburMaks.setCols(3);

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Lembur Dihitung Dari Awal Masuk"));
			final MyCheckboxConfig lemburDihitungDariAwalMasuk;
			rowKet.appendChild(lemburDihitungDariAwalMasuk = new MyCheckboxConfig("dari awal masuk kerja"));
			lemburDihitungDariAwalMasuk.setChecked(detailJenisShiftPegawai.getLemburDihitungDariAwalMasuk());

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Lembur Dihitung Dari Awal Masuk"));
			final MyCheckboxConfig jamMasukDanPulangOtomatisMenyesuakanWaktuShift;
			rowKet.appendChild(jamMasukDanPulangOtomatisMenyesuakanWaktuShift = new MyCheckboxConfig(
					"Jam Masuk Dan Pulang Otomatis Menyesuakan Waktu Shift"));
			jamMasukDanPulangOtomatisMenyesuakanWaktuShift
					.setChecked(detailJenisShiftPegawai.getJamMasukDanPulangOtomatisMenyesuakanWaktuShift());

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Konversi Jam Lembur dihitung mulai dari jam lembur:"));
			rowKet.appendChild(new MyLabelConfig());

			rowKet = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(rowKet, "2");
			rowKet.setParent(rowsKet);
			final MyTextbox konversiJamLembur;
			rowKet.appendChild(konversiJamLembur = new MyTextbox(detailJenisShiftPegawai.getKonversiJamLembur()));
			konversiJamLembur.setWidth("95%");
			konversiJamLembur.setRows(3);

			vbox.appendChild(new MyLabelStyled("Absensi Menggunakan Online"));
			Grid absenFoto = new Grid();
			absenFoto.setSclass("dgrid");
			absenFoto.setParent(vbox);
			absenFoto.setWidth("100%");
			absenFoto.setHeight("100%");

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Jadikan Default"));
			final MyCheckboxConfig jadikanDefault = new MyCheckboxConfig("Jadikan Default untuk absensi Online");
			jadikanDefault.setChecked(detailJenisShiftPegawai.getJadikanDefault());
			rowKet.appendChild(jadikanDefault);

			rowsKet = new Rows();
			rowsKet.setParent(absenFoto);

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Aktifkan"));
			final MyCheckboxConfig aktifkanAbsenFoto = new MyCheckboxConfig("Aktifkan absensi Online");
			aktifkanAbsenFoto.setChecked(detailJenisShiftPegawai.getAktifkanAbsenFoto());
			rowKet.appendChild(aktifkanAbsenFoto);

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Jumlah Menit Toleransi Sebelum Jam Datang"));
			final MyDoublebox menitSebelumJamMulai = new MyDoublebox(detailJenisShiftPegawai.getMenitSebelumJamMulai());
			rowKet.appendChild(menitSebelumJamMulai);

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Jumlah Menit Toleransi Setelah Jam Datang"));
			final MyDoublebox menitSetelahJamMulai = new MyDoublebox(detailJenisShiftPegawai.getMenitSetelahJamMulai());
			rowKet.appendChild(menitSetelahJamMulai);

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Jumlah Menit Toleransi Sebelum Jam Pulang"));
			final MyDoublebox menitSebelumJamSampai = new MyDoublebox(
					detailJenisShiftPegawai.getMenitSebelumJamSampai());
			rowKet.appendChild(menitSebelumJamSampai);

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Jumlah Menit Toleransi Setelah Jam Pulang"));
			final MyDoublebox menitSetelahJamSampai = new MyDoublebox(
					detailJenisShiftPegawai.getMenitSetelahJamSampai());
			rowKet.appendChild(menitSetelahJamSampai);

			vbox.appendChild(new MyLabelStyled("Status Terlambat/Cepat"));
			absenFoto = new Grid();
			absenFoto.setSclass("dgrid");
			absenFoto.setParent(vbox);
			absenFoto.setWidth("100%");
			absenFoto.setHeight("100%");

			rowsKet = new Rows();
			rowsKet.setParent(absenFoto);

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Jumlah Menit Toleransi Datang Cepat"));
			final MyDoublebox jamSebelumJamMulai = new MyDoublebox(detailJenisShiftPegawai.getJamSebelumJamMulai());
			rowKet.appendChild(jamSebelumJamMulai);

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Jumlah Menit Toleransi Datang Terlambat"));
			final MyDoublebox jamSetelahJamMulai = new MyDoublebox(detailJenisShiftPegawai.getJamSetelahJamMulai());
			rowKet.appendChild(jamSetelahJamMulai);

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Jumlah Menit Toleransi Pulang Cepat"));
			final MyDoublebox jamSebelumJamSampai = new MyDoublebox(detailJenisShiftPegawai.getJamSebelumJamSampai());
			rowKet.appendChild(jamSebelumJamSampai);

			rowKet = new MyFormRow();
			rowKet.setParent(rowsKet);
			rowKet.appendChild(new MyLabelConfig("Jumlah Menit Toleransi Pulang Terlambat"));
			final MyDoublebox jamSetelahJamSampai = new MyDoublebox(detailJenisShiftPegawai.getJamSetelahJamSampai());
			rowKet.appendChild(jamSetelahJamSampai);

			vbox.appendChild(new MyLabelStyled("Potongan Terlambat"));

			Grid terlambat = new Grid();
			terlambat.setSclass("dgrid");
			terlambat.setParent(vbox);
			terlambat.setWidth("100%");
			terlambat.setHeight("100%");

			Columns columnsTerlambat = new Columns();
			columnsTerlambat.setParent(terlambat);

			Column columnTerlambat = new Column("T1(M)");
			columnsTerlambat.appendChild(columnTerlambat);
			columnTerlambat = new Column("T1(%)");
			columnsTerlambat.appendChild(columnTerlambat);
			columnTerlambat = new Column("T2(M)");
			columnsTerlambat.appendChild(columnTerlambat);
			columnTerlambat = new Column("T2(%)");
			columnsTerlambat.appendChild(columnTerlambat);
			columnTerlambat = new Column("T3(M)");
			columnsTerlambat.appendChild(columnTerlambat);
			columnTerlambat = new Column("T3(%)");
			columnsTerlambat.appendChild(columnTerlambat);
			columnTerlambat = new Column("T4(M)");
			columnsTerlambat.appendChild(columnTerlambat);
			columnTerlambat = new Column("T4(%)");
			columnsTerlambat.appendChild(columnTerlambat);

			Rows rowsTerlambat = new Rows();
			rowsTerlambat.setParent(terlambat);

			MyFormRow rowTerlambat = new MyFormRow();
			rowTerlambat.setParent(rowsTerlambat);

			final MyDoublebox menitTelat1 = new MyDoublebox(detailJenisShiftPegawai.getMenitTelat1());
			menitTelat1.setWidth("90%");
			rowTerlambat.appendChild(menitTelat1);

			final MyDoublebox potonganTelat1 = new MyDoublebox(detailJenisShiftPegawai.getPotonganTelat1());
			potonganTelat1.setWidth("90%");
			rowTerlambat.appendChild(potonganTelat1);

			final MyDoublebox menitTelat2 = new MyDoublebox(detailJenisShiftPegawai.getMenitTelat2());
			menitTelat2.setWidth("90%");
			rowTerlambat.appendChild(menitTelat2);

			final MyDoublebox potonganTelat2 = new MyDoublebox(detailJenisShiftPegawai.getPotonganTelat2());
			potonganTelat2.setWidth("90%");
			rowTerlambat.appendChild(potonganTelat2);

			final MyDoublebox menitTelat3 = new MyDoublebox(detailJenisShiftPegawai.getMenitTelat3());
			menitTelat3.setWidth("90%");
			rowTerlambat.appendChild(menitTelat3);

			final MyDoublebox potonganTelat3 = new MyDoublebox(detailJenisShiftPegawai.getPotonganTelat3());
			potonganTelat3.setWidth("90%");
			rowTerlambat.appendChild(potonganTelat3);

			final MyDoublebox menitTelat4 = new MyDoublebox(detailJenisShiftPegawai.getMenitTelat4());
			menitTelat4.setWidth("90%");
			rowTerlambat.appendChild(menitTelat4);

			final MyDoublebox potonganTelat4 = new MyDoublebox(detailJenisShiftPegawai.getPotonganTelat4());
			potonganTelat4.setWidth("90%");
			rowTerlambat.appendChild(potonganTelat4);

			vbox.appendChild(new MyLabelStyled("Potongan Pulang Cepat"));

			Grid cepat = new Grid();
			cepat.setSclass("dgrid");
			cepat.setParent(vbox);
			cepat.setWidth("100%");
			cepat.setHeight("100%");

			Columns columnsCepat = new Columns();
			columnsCepat.setParent(cepat);

			Column columnCepat = new Column("C1(M)");
			columnsCepat.appendChild(columnCepat);
			columnCepat = new Column("C1(%)");
			columnsCepat.appendChild(columnCepat);
			columnCepat = new Column("C2(M)");
			columnsCepat.appendChild(columnCepat);
			columnCepat = new Column("C2(%)");
			columnsCepat.appendChild(columnCepat);
			columnCepat = new Column("C3(M)");
			columnsCepat.appendChild(columnCepat);
			columnCepat = new Column("C3(%)");
			columnsCepat.appendChild(columnCepat);
			columnCepat = new Column("C4(M)");
			columnsCepat.appendChild(columnCepat);
			columnCepat = new Column("C4(%)");
			columnsCepat.appendChild(columnCepat);

			Rows rowsCepat = new Rows();
			rowsCepat.setParent(cepat);

			MyFormRow rowCepat = new MyFormRow();
			rowCepat.setParent(rowsCepat);

			final MyDoublebox menitCepat1 = new MyDoublebox(detailJenisShiftPegawai.getMenitCepat1());
			menitCepat1.setWidth("90%");
			rowCepat.appendChild(menitCepat1);

			final MyDoublebox potonganCepat1 = new MyDoublebox(detailJenisShiftPegawai.getPotonganCepat1());
			potonganCepat1.setWidth("90%");
			rowCepat.appendChild(potonganCepat1);

			final MyDoublebox menitCepat2 = new MyDoublebox(detailJenisShiftPegawai.getMenitCepat2());
			menitCepat2.setWidth("90%");
			rowCepat.appendChild(menitCepat2);

			final MyDoublebox potonganCepat2 = new MyDoublebox(detailJenisShiftPegawai.getPotonganCepat2());
			potonganCepat2.setWidth("90%");
			rowCepat.appendChild(potonganCepat2);

			final MyDoublebox menitCepat3 = new MyDoublebox(detailJenisShiftPegawai.getMenitCepat3());
			menitCepat3.setWidth("90%");
			rowCepat.appendChild(menitCepat3);

			final MyDoublebox potonganCepat3 = new MyDoublebox(detailJenisShiftPegawai.getPotonganCepat3());
			potonganCepat3.setWidth("90%");
			rowCepat.appendChild(potonganCepat3);

			final MyDoublebox menitCepat4 = new MyDoublebox(detailJenisShiftPegawai.getMenitCepat4());
			menitCepat4.setWidth("90%");
			rowCepat.appendChild(menitCepat4);

			final MyDoublebox potonganCepat4 = new MyDoublebox(detailJenisShiftPegawai.getPotonganCepat4());
			potonganCepat4.setWidth("90%");
			rowCepat.appendChild(potonganCepat4);

			vbox.appendChild(new MyLabelStyled("Pot.TM(%)"));

			final MyDoublebox potonganTidakMasuk = new MyDoublebox(detailJenisShiftPegawai.getPotonganTidakMasuk());
			potonganTidakMasuk.setWidth("90%");
			vbox.appendChild(potonganTidakMasuk);

			EventListener listener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					WaktuShift w = (WaktuShift) (waktuShift.getSelectedItem() == null ? null
							: waktuShift.getSelectedItem().getValue());
					detailJenisShiftPegawai.setWaktuShift(w);
					detailJenisShiftPegawai.setHariKe(
							(Integer) (hariKe.getSelectedItem() == null ? null : hariKe.getSelectedItem().getValue()));

					detailJenisShiftPegawai.setAktifkanAbsenFoto(aktifkanAbsenFoto.isChecked());
					detailJenisShiftPegawai.setMenitSebelumJamMulai(menitSebelumJamMulai.getValue());
					detailJenisShiftPegawai.setMenitSetelahJamMulai(menitSetelahJamMulai.getValue());
					detailJenisShiftPegawai.setMenitSebelumJamSampai(menitSebelumJamSampai.getValue());
					detailJenisShiftPegawai.setMenitSetelahJamSampai(menitSetelahJamSampai.getValue());

					detailJenisShiftPegawai.setJamSebelumJamMulai(jamSebelumJamMulai.getValue());
					detailJenisShiftPegawai.setJamSetelahJamMulai(jamSetelahJamMulai.getValue());
					detailJenisShiftPegawai.setJamSebelumJamSampai(jamSebelumJamSampai.getValue());
					detailJenisShiftPegawai.setJamSetelahJamSampai(jamSetelahJamSampai.getValue());

					detailJenisShiftPegawai.setMulai(mulai.getValue());
					detailJenisShiftPegawai.setSampai(sampai.getValue());
					detailJenisShiftPegawai.setJenisShiftPegawai(jenisShiftPegawai);

					detailJenisShiftPegawai.setMenitTelat1(menitTelat1.getValue());
					detailJenisShiftPegawai.setPotonganTelat1(potonganTelat1.getValue());

					detailJenisShiftPegawai.setMenitTelat2(menitTelat2.getValue());
					detailJenisShiftPegawai.setPotonganTelat2(potonganTelat2.getValue());

					detailJenisShiftPegawai.setMenitTelat3(menitTelat3.getValue());
					detailJenisShiftPegawai.setPotonganTelat3(potonganTelat3.getValue());

					detailJenisShiftPegawai.setMenitTelat4(menitTelat4.getValue());
					detailJenisShiftPegawai.setPotonganTelat4(potonganTelat4.getValue());

					detailJenisShiftPegawai.setMenitCepat1(menitCepat1.getValue());
					detailJenisShiftPegawai.setPotonganCepat1(potonganCepat1.getValue());

					detailJenisShiftPegawai.setMenitCepat2(menitCepat2.getValue());
					detailJenisShiftPegawai.setPotonganCepat2(potonganCepat2.getValue());

					detailJenisShiftPegawai.setMenitCepat3(menitCepat3.getValue());
					detailJenisShiftPegawai.setPotonganCepat3(potonganCepat3.getValue());

					detailJenisShiftPegawai.setMenitCepat4(menitCepat4.getValue());
					detailJenisShiftPegawai.setPotonganCepat4(potonganCepat4.getValue());
					detailJenisShiftPegawai.setPotonganTidakMasuk(potonganTidakMasuk.getValue());
					detailJenisShiftPegawai.setLemburMulai(lemburMulai.getValue());
					detailJenisShiftPegawai.setLemburMaks(lemburMaks.getValue());
					detailJenisShiftPegawai.setHari(
							(String) (hari.getSelectedItem() == null ? null : hari.getSelectedItem().getValue()));

					detailJenisShiftPegawai.setKonversiJamLembur(konversiJamLembur.getValue().trim());
					detailJenisShiftPegawai.setKhususBuatHariLibur(khususBuatHariLibur.isChecked());
					detailJenisShiftPegawai.setLemburDihitungDariAwalMasuk(lemburDihitungDariAwalMasuk.isChecked());
					detailJenisShiftPegawai.setJamMasukDanPulangOtomatisMenyesuakanWaktuShift(
							jamMasukDanPulangOtomatisMenyesuakanWaktuShift.isChecked());

					detailJenisShiftPegawai.setJadikanDefault(jadikanDefault.isChecked());

					tabSoal.setAttribute("detailJenisShiftPegawai", detailJenisShiftPegawai);

					if (jenisShiftPegawai != null && jenisShiftPegawai.getId() != null) {
						Common.refreshSaveOrUpdate(detailJenisShiftPegawai);
					}
				}
			};
			waktuShift.addEventListener("onChange", listener);
			hari.addEventListener("onChange", listener);
			mulai.addEventListener("onChange", listener);
			sampai.addEventListener("onChange", listener);

			lemburMulai.addEventListener("onChange", listener);
			lemburMaks.addEventListener("onChange", listener);

			menitTelat1.addEventListener("onChange", listener);
			potonganTelat1.addEventListener("onChange", listener);

			menitTelat2.addEventListener("onChange", listener);
			potonganTelat2.addEventListener("onChange", listener);

			menitTelat3.addEventListener("onChange", listener);
			potonganTelat3.addEventListener("onChange", listener);

			menitTelat4.addEventListener("onChange", listener);
			potonganTelat4.addEventListener("onChange", listener);

			menitCepat1.addEventListener("onChange", listener);
			potonganCepat1.addEventListener("onChange", listener);

			menitCepat2.addEventListener("onChange", listener);
			potonganCepat2.addEventListener("onChange", listener);

			menitCepat3.addEventListener("onChange", listener);
			potonganCepat3.addEventListener("onChange", listener);

			menitCepat4.addEventListener("onChange", listener);
			potonganCepat4.addEventListener("onChange", listener);

			potonganTidakMasuk.addEventListener("onChange", listener);
			aktifkanAbsenFoto.addEventListener("onClick", listener);
			menitSebelumJamMulai.addEventListener("onChange", listener);
			menitSetelahJamMulai.addEventListener("onChange", listener);
			menitSebelumJamSampai.addEventListener("onChange", listener);
			menitSetelahJamSampai.addEventListener("onChange", listener);

			jamSebelumJamMulai.addEventListener("onChange", listener);
			jamSetelahJamMulai.addEventListener("onChange", listener);
			jamSebelumJamSampai.addEventListener("onChange", listener);
			jamSetelahJamSampai.addEventListener("onChange", listener);
			jamMasukDanPulangOtomatisMenyesuakanWaktuShift.addEventListener("onClick", listener);
			hariKe.addEventListener("onChange", listener);

			konversiJamLembur.addEventListener("onChange", listener);
			khususBuatHariLibur.addEventListener("onClick", listener);
			lemburDihitungDariAwalMasuk.addEventListener("onClick", listener);
			jadikanDefault.addEventListener("onClick", listener);

			eventListenerLibur.onEvent(null);

			if (jenisShiftPegawai != null && jenisShiftPegawai.getId() != null) {
				if (detailJenisShiftPegawai != null && detailJenisShiftPegawai.getId() == null) {
					listener.onEvent(null);
				}
			}
		}
	}

	private void init(final JenisShiftPegawai jenisShiftPegawai) throws Exception {
		this.jenisShiftPegawai = jenisShiftPegawai;
		addWindow.setTitle(jenisShiftPegawai.getId() == null ? "Tambah Jenis Shift Pegawai" : "Ubah Jenis Shift Pegawai");
		Common.clear(addWindow);
		addWindow.setHeight("95%");
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		final East east = new East();
		east.setParent(borderlayout);
		east.setWidth("75%");
		initShiftDetail(east, jenisShiftPegawai);
		ais.ui.util.ZkCompat.setFlex(east, true);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama Shift")));
		row.appendChild(nama = new MyTextbox(jenisShiftPegawai.getNama() == null ? "" : jenisShiftPegawai.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Jumlah Shift")));
		row.appendChild(jumlahShift = new Intbox(jenisShiftPegawai.getJumlahShift()));
		jumlahShift.setWidth("90%");
		jumlahShift.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jenisShiftPegawai.setJumlahShift(jumlahShift.getValue());
				initShiftDetail(east, jenisShiftPegawai);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Berlaku mulai")));
		row.appendChild(berlakuMulai = new MyDatebox(jenisShiftPegawai.getBerlakuMulai()));
		berlakuMulai.setWidth("90%");
		berlakuMulai.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Berlaku sampai")));
		row.appendChild(berlakuSampai = new MyDatebox(jenisShiftPegawai.getBerlakuSampai()));
		berlakuSampai.setWidth("90%");

		Common.initKeterangan(rows,
				"Kosongkan masa berlaku sampai jika berlaku selamanta atau belum ditentukan batas waktunya");

		Tbmuser tbmuser1 = Common.getCurrentUser();

		fakultas = new Combobox();
		jurusan = new Combobox();
		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas,
				jenisShiftPegawai.getFakultas() == null ? tbmuser1.ambilFakultas() : jenisShiftPegawai.getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		if (fakultas.getSelectedItem() != null && fakultas.getSelectedItem().getValue() != null) {
			Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
					CommonSearchFilterHelper.eqSelectedWithId("fakultas", fakultas, false));
		}

		row = new MyFormRow();
		row.setVisible(pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				jenisShiftPegawai.getJurusan() == null ? tbmuser1.ambilJurusan() : jenisShiftPegawai.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");

		yayasan = new Combobox();
		sekolah = new Combobox();
		Common.initYayasanDanSekolahDanSemua(yayasan, sekolah, null, null);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Yayasan"));

		Common.selectComboItem(yayasan,
				jenisShiftPegawai == null || jenisShiftPegawai.getYayasan() == null ? tbmuser1.ambilYayasan()
						: jenisShiftPegawai.getYayasan());
		row.appendChild(yayasan);
		yayasan.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Sekolah"));

		Common.pilihSekolah(sekolah,
				jenisShiftPegawai == null || jenisShiftPegawai.getSekolah() == null ? tbmuser1.ambilSekolah()
						: jenisShiftPegawai.getSekolah());
		row.appendChild(sekolah);
		sekolah.setWidth("90%");

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi I"));
		row.appendChild(lokasi = new Combobox());
		lokasi.setWidth("90%");
		Common.insertComboDanSemua(lokasi, new String[] { "nama", "lat", "lng" }, "alamat", Lokasi.class,
				"Tidak Ditentukan", Restrictions.eq("aktif", true));
		Common.selectComboItem(lokasi, jenisShiftPegawai.getLokasi());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi II"));
		row.appendChild(lokasi2 = new Combobox());
		lokasi2.setWidth("90%");
		Common.insertComboDanSemua(lokasi2, new String[] { "nama", "lat", "lng" }, "alamat", Lokasi.class,
				"Tidak Ditentukan", Restrictions.eq("aktif", true));
		Common.selectComboItem(lokasi2, jenisShiftPegawai.getLokasi2());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi III"));
		row.appendChild(lokasi3 = new Combobox());
		lokasi3.setWidth("90%");
		Common.insertComboDanSemua(lokasi3, new String[] { "nama", "lat", "lng" }, "alamat", Lokasi.class,
				"Tidak Ditentukan", Restrictions.eq("aktif", true));
		Common.selectComboItem(lokasi3, jenisShiftPegawai.getLokasi3());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi IV"));
		row.appendChild(lokasi4 = new Combobox());
		lokasi4.setWidth("90%");
		Common.insertComboDanSemua(lokasi4, new String[] { "nama", "lat", "lng" }, "alamat", Lokasi.class,
				"Tidak Ditentukan", Restrictions.eq("aktif", true));
		Common.selectComboItem(lokasi4, jenisShiftPegawai.getLokasi4());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi V"));
		row.appendChild(lokasi5 = new Combobox());
		lokasi5.setWidth("90%");
		Common.insertComboDanSemua(lokasi5, new String[] { "nama", "lat", "lng" }, "alamat", Lokasi.class,
				"Tidak Ditentukan", Restrictions.eq("aktif", true));
		Common.selectComboItem(lokasi5, jenisShiftPegawai.getLokasi5());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi VI"));
		row.appendChild(lokasi6 = new Combobox());
		lokasi6.setWidth("90%");
		Common.insertComboDanSemua(lokasi6, new String[] { "nama", "lat", "lng" }, "alamat", Lokasi.class,
				"Tidak Ditentukan", Restrictions.eq("aktif", true));
		Common.selectComboItem(lokasi6, jenisShiftPegawai.getLokasi6());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi VII"));
		row.appendChild(lokasi7 = new Combobox());
		lokasi7.setWidth("90%");
		Common.insertComboDanSemua(lokasi7, new String[] { "nama", "lat", "lng" }, "alamat", Lokasi.class,
				"Tidak Ditentukan", Restrictions.eq("aktif", true));
		Common.selectComboItem(lokasi7, jenisShiftPegawai.getLokasi7());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi VIII"));
		row.appendChild(lokasi8 = new Combobox());
		lokasi8.setWidth("90%");
		Common.insertComboDanSemua(lokasi8, new String[] { "nama", "lat", "lng" }, "alamat", Lokasi.class,
				"Tidak Ditentukan", Restrictions.eq("aktif", true));
		Common.selectComboItem(lokasi8, jenisShiftPegawai.getLokasi8());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi IX"));
		row.appendChild(lokasi9 = new Combobox());
		lokasi9.setWidth("90%");
		Common.insertComboDanSemua(lokasi9, new String[] { "nama", "lat", "lng" }, "alamat", Lokasi.class,
				"Tidak Ditentukan", Restrictions.eq("aktif", true));
		Common.selectComboItem(lokasi9, jenisShiftPegawai.getLokasi9());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Lokasi X"));
		row.appendChild(lokasi10 = new Combobox());
		lokasi10.setWidth("90%");
		Common.insertComboDanSemua(lokasi10, new String[] { "nama", "lat", "lng" }, "alamat", Lokasi.class,
				"Tidak Ditentukan", Restrictions.eq("aktif", true));
		Common.selectComboItem(lokasi10, jenisShiftPegawai.getLokasi10());

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Radius posisi kehadiran titik dari lokasi (km)"));
		row.appendChild(jarak = new MyDoublebox(jenisShiftPegawai.getJarak()));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Waktu minimal bekerja (jam)"));
		row.appendChild(waktuBekerjaMinimal = new MyDoublebox(jenisShiftPegawai.getWaktuBekerjaMinimal()));

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(berotasi = new MyCheckboxConfig("Shift ini berotasi"));
		berotasi.setChecked(jenisShiftPegawai.getBerotasi());
		berotasi.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jenisShiftPegawai.setBerotasi(berotasi.isChecked());
				initShiftDetail(east, jenisShiftPegawai);

				jumlahHariSamaDenganJumlahShift.getParent().setVisible(berotasi.isChecked());
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label());
		jumlahHariSamaDenganJumlahShift = new MyCheckboxConfig("Jumlah hari sama dengan jumlah shift");
		jumlahHariSamaDenganJumlahShift.setChecked(jenisShiftPegawai.getJumlahHariSamaDenganJumlahShift());
		jumlahHariSamaDenganJumlahShift.setParent(row);
		jumlahHariSamaDenganJumlahShift.getParent().setVisible(berotasi.isChecked());
		jumlahHariSamaDenganJumlahShift.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jenisShiftPegawai.setJumlahHariSamaDenganJumlahShift(jumlahHariSamaDenganJumlahShift.isChecked());
				initShiftDetail(east, jenisShiftPegawai);

				jumlahHari.getParent().setVisible(!jumlahHariSamaDenganJumlahShift.isChecked());
			}
		});

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.setValign("top");
		row.appendChild(new ais.ui.util.MyLabelConfig("Jumlah hari berotasi"));
		row.appendChild(jumlahHari = new MyIntbox(jenisShiftPegawai.getJumlahHari()));
		jumlahHari.getParent().setVisible(!jumlahHariSamaDenganJumlahShift.isChecked());
		jumlahHari.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jenisShiftPegawai.setJumlahHari(jumlahHari.getValue());
				initShiftDetail(east, jenisShiftPegawai);
			}
		});

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(hariLiburDitentukan = new MyCheckboxConfig("Hari libur ditentukan di shift"));
		hariLiburDitentukan.setChecked(jenisShiftPegawai.getHariLiburDitentukan());
		hariLiburDitentukan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jenisShiftPegawai.setHariLiburDitentukan(hariLiburDitentukan.isChecked());
				initShiftDetail(east, jenisShiftPegawai);
			}
		});

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label());
		row.appendChild(
				harusMengikutiStateMasukDanPulang = new MyCheckboxConfig("Harus Mengikuti State Masuk Dan Pulang"));
		harusMengikutiStateMasukDanPulang.setChecked(jenisShiftPegawai.getHarusMengikutiStateMasukDanPulang());
		harusMengikutiStateMasukDanPulang.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				jenisShiftPegawai.setHarusMengikutiStateMasukDanPulang(harusMengikutiStateMasukDanPulang.isChecked());
				initShiftDetail(east, jenisShiftPegawai);
			}
		});

		row = new MyFormRow();
		row.setVisible(ya);
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label());
		defaultAbsenGuru = new MyCheckboxConfig("Default untuk guru");
		defaultAbsenGuru.setDisabled(!edit);
		defaultAbsenGuru.setChecked(jenisShiftPegawai.getDefaultAbsenGuru());
		defaultAbsenGuru.setParent(row);

		row = new MyFormRow();
		row.setVisible(pt);
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label());
		defaultAbsenDosen = new MyCheckboxConfig("Default untuk dosen");
		defaultAbsenDosen.setDisabled(!edit);
		defaultAbsenDosen.setChecked(jenisShiftPegawai.getDefaultAbsenDosen());
		defaultAbsenDosen.setParent(row);

		row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label());
		defaultAbsenPegawai = new MyCheckboxConfig("Default untuk pegawai");
		defaultAbsenPegawai.setDisabled(!edit);
		defaultAbsenPegawai.setChecked(jenisShiftPegawai.getDefaultAbsenPegawai());
		defaultAbsenPegawai.setParent(row);

		row = new MyFormRow();
		row.setVisible(pt);
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label());
		defaultMahasiswa = new MyCheckboxConfig("Default untuk mahasiswa");
		defaultMahasiswa.setDisabled(!edit);
		defaultMahasiswa.setChecked(jenisShiftPegawai.getDefaultMahasiswa());
		defaultMahasiswa.setParent(row);

		row = new MyFormRow();
		row.setVisible(ya);
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new Label());
		defaultSiswa = new MyCheckboxConfig("Default untuk siswa");
		defaultSiswa.setDisabled(!edit);
		defaultSiswa.setChecked(jenisShiftPegawai.getDefaultSiswa());
		defaultSiswa.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("Keterangan")));
		row.appendChild(keterangan = new MyTextbox(
				jenisShiftPegawai.getKeterangan() == null ? "" : jenisShiftPegawai.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setHeight("30px");
		toolbar.setParent(south);
		Toolbarbutton cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		Toolbarbutton save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
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
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	@SuppressWarnings("unchecked")
	public boolean onSave(Event event) throws Exception {
		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon maaf, kolom Nama Jenis Shift wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) isikan Nama Jenis Shift pada kolom yang tersedia; (2) pastikan Nama tidak dikosongkan; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		if (berlakuMulai.getValue() == null) {
			MyMessageboxConfig.show(
					"Mohon maaf, Tanggal dan waktu berlaku mulai wajib diisi terlebih dahulu. Langkah yang dapat dilakukan: (1) tentukan Tanggal dan waktu berlaku mulai pada kolom yang tersedia; (2) pastikan kolom tidak dikosongkan; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		/*
		 * if (keterangan.getValue().trim().equals("")) { Messagebox.show(
		 * "Keterangan harus diisi", "Peringatan", Messagebox.OK,
		 * Messagebox.EXCLAMATION); return false; }
		 */

		boolean i = checkNamaJenisShiftPegawai();
		if (i) {
			MyMessageboxConfig.show(
					"Mohon maaf, Nama Jenis Shift yang Bapak/Ibu masukkan sudah terdaftar di dalam basis data. Langkah yang dapat dilakukan: (1) gunakan Nama Jenis Shift yang berbeda; (2) periksa kembali daftar Jenis Shift yang telah ada; (3) simpan kembali data ini.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();
		if (jenisShiftPegawai.getId() != null) {
			jenisShiftPegawai = (JenisShiftPegawai) session.load(JenisShiftPegawai.class, jenisShiftPegawai.getId());

		}
		jenisShiftPegawai.setWaktuBekerjaMinimal(waktuBekerjaMinimal.getValue());
		jenisShiftPegawai.setBerlakuMulai(berlakuMulai.getValue());
		jenisShiftPegawai.setBerlakuSampai(berlakuSampai.getValue());
		jenisShiftPegawai.setJumlahShift(jumlahShift.getValue());
		jenisShiftPegawai.setNama(nama.getValue());
		jenisShiftPegawai.setKeterangan(keterangan.getValue());
		jenisShiftPegawai.setLokasi((Lokasi) (lokasi == null || lokasi.getSelectedItem() == null ? null
				: lokasi.getSelectedItem().getValue()));

		jenisShiftPegawai.setLokasi2((Lokasi) (lokasi2 == null || lokasi2.getSelectedItem() == null ? null
				: lokasi2.getSelectedItem().getValue()));

		jenisShiftPegawai.setLokasi3((Lokasi) (lokasi3 == null || lokasi3.getSelectedItem() == null ? null
				: lokasi3.getSelectedItem().getValue()));

		jenisShiftPegawai.setLokasi4((Lokasi) (lokasi4 == null || lokasi4.getSelectedItem() == null ? null
				: lokasi4.getSelectedItem().getValue()));

		jenisShiftPegawai.setLokasi5((Lokasi) (lokasi5 == null || lokasi5.getSelectedItem() == null ? null
				: lokasi5.getSelectedItem().getValue()));

		jenisShiftPegawai.setLokasi6((Lokasi) (lokasi6 == null || lokasi6.getSelectedItem() == null ? null
				: lokasi6.getSelectedItem().getValue()));

		jenisShiftPegawai.setLokasi7((Lokasi) (lokasi7 == null || lokasi7.getSelectedItem() == null ? null
				: lokasi7.getSelectedItem().getValue()));

		jenisShiftPegawai.setLokasi8((Lokasi) (lokasi8 == null || lokasi8.getSelectedItem() == null ? null
				: lokasi8.getSelectedItem().getValue()));

		jenisShiftPegawai.setLokasi9((Lokasi) (lokasi9 == null || lokasi9.getSelectedItem() == null ? null
				: lokasi9.getSelectedItem().getValue()));

		jenisShiftPegawai.setLokasi10((Lokasi) (lokasi10 == null || lokasi10.getSelectedItem() == null ? null
				: lokasi10.getSelectedItem().getValue()));

		jenisShiftPegawai.setJarak(jarak == null ? null : jarak.getValue());

		jenisShiftPegawai.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		jenisShiftPegawai.setFakultas(
				(Fakultas) (fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null ? null
						: fakultas.getSelectedItem().getValue()));

		jenisShiftPegawai.setYayasan(
				(Yayasan) (yayasan.getSelectedItem() == null ? null : yayasan.getSelectedItem().getValue()));
		jenisShiftPegawai.setSekolah(
				(Sekolah) (sekolah.getSelectedItem() == null ? null : sekolah.getSelectedItem().getValue()));

		jenisShiftPegawai.setBerotasi(berotasi.isChecked());

		jenisShiftPegawai.setDefaultAbsenGuru(defaultAbsenGuru.isChecked());
		jenisShiftPegawai.setDefaultAbsenDosen(defaultAbsenDosen.isChecked());
		jenisShiftPegawai.setDefaultAbsenPegawai(defaultAbsenPegawai.isChecked());
		jenisShiftPegawai.setDefaultMahasiswa(defaultMahasiswa.isChecked());
		jenisShiftPegawai.setDefaultSiswa(defaultSiswa.isChecked());
		jenisShiftPegawai.setHariLiburDitentukan(hariLiburDitentukan.isChecked());
		jenisShiftPegawai.setJumlahHariSamaDenganJumlahShift(jumlahHariSamaDenganJumlahShift.isChecked());
		jenisShiftPegawai.setJumlahHari(jumlahHari.getValue());

		jenisShiftPegawai.setHarusMengikutiStateMasukDanPulang(harusMengikutiStateMasukDanPulang.isChecked());

		Common.refreshSaveOrUpdate(session, jenisShiftPegawai);

		List<Tab> t = ((Component) tabs).getChildren();
		for (Tab tab : t) {
			DetailJenisShiftPegawai detailJenisShiftPegawai = (DetailJenisShiftPegawai) tab
					.getAttribute("detailJenisShiftPegawai");
			detailJenisShiftPegawai.setJenisShiftPegawai(jenisShiftPegawai);

			Common.refreshSaveOrUpdate(session, detailJenisShiftPegawai);
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);
		List<JenisShiftPegawai> jenisShiftPegawai = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(jenisShiftPegawai);
		grid.setRowRenderer(new JenisShiftPegawaiRenderer());
		grid.setModelCheckMobile(strset);

		grid.renderAll();

	}

	private Criteria initCriteria(boolean order) {

		boolean admin = Common.getApakahAdmin();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(JenisShiftPegawai.class)

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: !admin ? CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)
										: Restrictions.or(Restrictions.isNull("jurusan"),
												CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false)))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: !admin ? CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)
										: Restrictions.or(Restrictions.isNull("fakultas"),
												CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false)))

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: !admin ? CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false)
										: Restrictions.or(Restrictions.isNull("sekolah"),
												CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false)))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: !admin ? CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)
										: Restrictions.or(Restrictions.isNull("yayasan"),
												CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false)))

				.add(searchaktif == null || searchaktif.isChecked()
						? Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add((searchnama == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchnama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
				: Restrictions.ilike("nama", searchnama.getValue().trim(), MatchMode.ANYWHERE)));
		return criteria;
	}

	public Boolean checkNamaJenisShiftPegawai() {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(JenisShiftPegawai.class).setProjection(Projections.rowCount())
				.add(Restrictions.eq("nama", nama.getValue().trim()))
				.add(this.jenisShiftPegawai.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.jenisShiftPegawai.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

}
