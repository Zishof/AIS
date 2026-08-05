package ais.action.master;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.West;

import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataNegaraBanbox;
import ais.action.master.helper.DetailwisudaHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.report.Report;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.dao.DaoFactory;
import ais.database.dao.MahasiswaDao;
import ais.database.dao.PendaftaranWisudaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Agama;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.Jenjang;
import ais.database.model.Judisium;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Negara;
import ais.database.model.PendaftaranWisuda;
import ais.database.model.Perkuliahan;
import ais.database.model.Skripsi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.Wisuda;
import ais.database.model.file.FotoMahasiswa;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyHboxToolbar;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.action.master.helper.FilterLanjutHelper;

public class MahasiswaRegistrasiWisudaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3786091220301468178L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private MyLabelConfig labelPersetujuanAdministrasi;
	private MyLabelConfig labelPersetujuanAdministrasiFakultas;
	private MyLabelConfig labelPersetujuanKeuangan;
	private MyLabelConfig labelPersetujuanPerpustakaan;
	private MyLabelConfig labelPersetujuanPerpustakaanFakultas;
	private MyLabelConfig labelPersetujuanWisuda;

	private Textbox ktp;
	private Textbox nama;
	private Textbox alamat;
	private Textbox email;
	private Decimalbox tahunangkatan;
	private Textbox tempatlahir;
	private MyDatebox tanggallahir;
	private Combobox kelamin;
	private Textbox telp;
	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox jenjang;
	private Combobox program;
	private AmbilDataNegaraBanbox negara;
	private Combobox semesterMulai;
	private Combobox agama;
	private Combobox statusAwalMahasiswa;
	private Decimalbox berat_badan;
	private Decimalbox tinggi_badan;
	private Textbox golongan_darah;

	private Textbox searchmahasiswa;
	private Textbox searchnim;
	private MyCheckboxConfig searchPersetujuanAdministrasi;
	private MyCheckboxConfig searchPersetujuanAdministrasiFakultas;
	private MyCheckboxConfig searchPersetujuanKeuangan;
	private MyCheckboxConfig searchPersetujuanPerpustakaan;
	private MyCheckboxConfig searchPersetujuanPerpustakaanFakultas;
	private MyCheckboxConfig searchPersetujuanWisuda;
	private Combobox searchwisuda;

	// private Combobox mahasiswa;
	private Mahasiswa mahasiswa;
	private Label nim;
	private Textbox keterangan;
	private Textbox noKursi;
	private Textbox noRegistrasiWisuda;
	private MyCheckboxConfig statusPersetujuanKeuangan;
	private MyCheckboxConfig statusPersetujuanAdministrasi;
	private MyCheckboxConfig statusPersetujuanPerpustakaan;
	private MyCheckboxConfig statusPersetujuanPerpustakaanFakultas;
	private MyCheckboxConfig statusPersetujuanAdministrasiFakultas;
	private PendaftaranWisuda pendaftaranWisuda;

	private boolean edit = false;
	private boolean delete = false;
	private MyCheckboxConfig persetujuanWisuda;

	private Image foto;
	private Combobox kewarganegaraan;

	private BiodataMahasiswa biodataMahasiswa;
	private AmbilDataDosenBanbox dosen;
	private Borderlayout mahasiswaPanel;
	private EventListener eventListener;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		if (searchwisuda != null) {
			Common.insertCombo(searchwisuda, new String[] { "wisudaKe", "moto", "keterangan", "maksimalQuota" },
					Wisuda.class, Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);

		if (labelPersetujuanWisuda != null) { labelPersetujuanWisuda.setMultiline(true); }
		if (labelPersetujuanWisuda != null) { labelPersetujuanWisuda.setValue("Persetujuan Wisuda"); }

		if (labelPersetujuanAdministrasi != null) { labelPersetujuanAdministrasi.setMultiline(true); }
		if (labelPersetujuanAdministrasi != null) { labelPersetujuanAdministrasi.setValue("Persetujuan Administrasi"); }

		if (labelPersetujuanAdministrasiFakultas != null) { labelPersetujuanAdministrasiFakultas.setMultiline(true); }
		if (labelPersetujuanAdministrasiFakultas != null) { labelPersetujuanAdministrasiFakultas.setValue("Persetujuan Administrasi Fakultas"); }

		if (labelPersetujuanKeuangan != null) { labelPersetujuanKeuangan.setMultiline(true); }
		if (labelPersetujuanKeuangan != null) { labelPersetujuanKeuangan.setValue("Persetujuan Keuangan"); }

		if (labelPersetujuanPerpustakaan != null) { labelPersetujuanPerpustakaan.setMultiline(true); }
		if (labelPersetujuanPerpustakaan != null) { labelPersetujuanPerpustakaan.setValue("Persetujuan Perpustakaan"); }

		if (labelPersetujuanPerpustakaanFakultas != null) { labelPersetujuanPerpustakaanFakultas.setMultiline(true); }
		if (labelPersetujuanPerpustakaanFakultas != null) { labelPersetujuanPerpustakaanFakultas.setValue("Persetujuan Perpustakaan Fakultas"); }

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	        FilterLanjutHelper.setup(comp);
}

	public static void onAddExternal(Event event, EventListener eventListener, PendaftaranWisuda pendaftaranWisuda)
			throws Exception {
		MahasiswaRegistrasiWisudaAction pendaftaranWisudaAction = new MahasiswaRegistrasiWisudaAction();
		pendaftaranWisudaAction.edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		pendaftaranWisudaAction.delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		pendaftaranWisudaAction.eventListener = eventListener;
		pendaftaranWisudaAction.addWindow = new MyWindow();

		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(pendaftaranWisudaAction.addWindow);
		pendaftaranWisudaAction.addWindow.setHeight("97%");
		pendaftaranWisudaAction.addWindow.setWidth("90%");

		pendaftaranWisudaAction.init(pendaftaranWisuda);

		pendaftaranWisudaAction.addWindow.setVisible(true);
		pendaftaranWisudaAction.addWindow.onModal();
	}

	@SuppressWarnings("unchecked")
	private Borderlayout initMain(final Mahasiswa mahasiswa) throws Exception {
		this.mahasiswa = mahasiswa;
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		biodataMahasiswa = mahasiswa.ambilBiodata();

		kewarganegaraan = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel(ais.database.model.Mahasiswa.WNI);
		comboitem.setValue(ais.database.model.Mahasiswa.WNI);
		kewarganegaraan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(ais.database.model.Mahasiswa.WNA);
		comboitem.setValue(ais.database.model.Mahasiswa.WNA);
		kewarganegaraan.appendChild(comboitem);

		kelamin = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Laki-laki");
		comboitem.setValue("Laki-laki");
		kelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Perempuan");
		comboitem.setValue("Perempuan");
		kelamin.appendChild(comboitem);

		semesterMulai = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		semesterMulai.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		semesterMulai.appendChild(comboitem);

		program = Common.initPrograms(program);

		West west = new West();
		west.setStyle("border:0px;");
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("250px");
		west.setParent(borderlayout);

		Vbox vbox = new Vbox();
		vbox.setPack("center");
		vbox.setAlign("center");
		vbox.setHeight("100%");
		vbox.setWidth("100%");
		vbox.setParent(west);
		vbox.appendChild(foto = new Image("/img/administrator-icon_default.png"));
		// foto.setHeight("300px");
		foto.setWidth("250px");
		MyToolbarbuttonConfig fileupload = new MyToolbarbuttonConfig("Ganti Foto" + Common.ukuranLabelFileUpload(),
				"/img/File-Upload-icon.png");
		fileupload.setUpload(Common.ukuranFileUpload());
		vbox.appendChild(fileupload);
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				try {
					UploadEvent uploadEvent = (UploadEvent) event;
					if (uploadEvent != null) {

						Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
						FotoMahasiswa fotoMahasiswa = (FotoMahasiswa) streamingSession
								.createCriteria(FotoMahasiswa.class).addOrder(Order.desc("id"))
								.add(Restrictions.eq("mahasiswa", mahasiswa.getId())).setMaxResults(1).uniqueResult();
						if (fotoMahasiswa != null) {
							streamingSession.getTransaction().begin();
							streamingSession.delete(fotoMahasiswa);
							streamingSession.getTransaction().commit();
						}

						fotoMahasiswa = new FotoMahasiswa();
						fotoMahasiswa.setNama(uploadEvent.getMedia().getName());
						fotoMahasiswa.setKeterangan(uploadEvent.getMedia().getContentType());
						fotoMahasiswa.setMahasiswa(mahasiswa.getId());

						fotoMahasiswa.setFoto(Common.getBlobFromMedia(uploadEvent.getMedia()));

						streamingSession.getTransaction().begin();
						streamingSession.save(fotoMahasiswa);
						streamingSession.getTransaction().commit();

						StreamingHibernateUtil.getInstance().closeSession();

						foto.setSrc(CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa)));
					} else {
						if (mahasiswa.getId() != null) {
							foto.setSrc(CommonMedia.getUrlFotoPengguna(new Tbmuser(mahasiswa)));
						}
					}
				} catch (Exception e) {
					StreamingHibernateUtil.getInstance().rollbackTransaction();
				}

			}
		};
		fileupload.addEventListener("onUpload", eventListener);

		eventListener.onEvent(null);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nim = new Label(mahasiswa.getNim() == null ? "" : mahasiswa.getNim()));
		nim.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox(mahasiswa.getNama() == null ? "" : mahasiswa.getNama()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunangkatan = new Decimalbox(new BigDecimal(
				mahasiswa.getTahunangkatan() == null ? ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR)
						: mahasiswa.getTahunangkatan())));
		tahunangkatan.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		Common.selectComboItem(program,
				mahasiswa.getProgram() == null || mahasiswa.getProgram().trim().equals("") ? "Reguler"
						: mahasiswa.getProgram());
		row.appendChild(program);
		program.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kewarganegaraan"));
		kewarganegaraan = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(ais.database.model.Mahasiswa.WNI);
		comboitem.setValue(ais.database.model.Mahasiswa.WNI);
		kewarganegaraan.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel(ais.database.model.Mahasiswa.WNA);
		comboitem.setValue(ais.database.model.Mahasiswa.WNA);
		kewarganegaraan.appendChild(comboitem);
		Common.selectComboItem(kewarganegaraan, mahasiswa.getWarganegara());
		row.appendChild(kewarganegaraan);
		kewarganegaraan.setWidth("90%");

		fakultas = new Combobox();
		jurusan = new Combobox();

		Common.initFakultasDanJurusan(fakultas, jurusan, null, null);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Asal Negara"));
		row.appendChild(negara = new AmbilDataNegaraBanbox());
		
		
		try {
			negara.setAttribute("negara", mahasiswa.getNegara() == null ? ConstantValues.INDONESIA : mahasiswa.getNegara());
			negara.setValue(
					(mahasiswa.getNegara() == null ? ConstantValues.INDONESIA : mahasiswa.getNegara()).getNamaNegara());
		}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/MahasiswaRegistrasiWisudaAction.java:404");
			// TODO: handle exception
		}
		
		negara.setWidth("90%");

		Tbmuser tbmuser = Common.getCurrentUser();

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		Common.selectComboItem(fakultas,
				mahasiswa.getJurusan() == null ? tbmuser.ambilFakultas() : mahasiswa.getJurusan().getFakultas());
		row.appendChild(fakultas);
		fakultas.setWidth("90%");

		Common.insertCombo(jurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
				Restrictions.eq("fakultas", mahasiswa.getJurusan() == null ? tbmuser.ambilFakultas()
						: mahasiswa.getJurusan().getFakultas()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		Common.pilihJurusan(jurusan,
				mahasiswa.getJurusan() == null ? tbmuser.ambilJurusan() : mahasiswa.getJurusan());
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		// jurusan.setDisabled(false);

		KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen Pembimbing Akademik"));
		row.appendChild(dosen = new AmbilDataDosenBanbox());
		dosen.setValue(
				krsMahasiswa == null || krsMahasiswa.getDosenPa() == null ? "" : (krsMahasiswa.getDosenPa().getNama()));
		dosen.setAttribute("dosen", krsMahasiswa == null ? null : krsMahasiswa.getDosenPa());
		dosen.setAttribute("myValue", krsMahasiswa == null ? null : krsMahasiswa.getDosenPa());
		dosen.setWidth("90%");

		if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen mydosen = tbmuser.ambilDosen();
			dosen.setValue(mydosen.getNama());
			dosen.setAttribute("myValue", mydosen);
			dosen.setAttribute("dosen", mydosen);
			dosen.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Kelamin"));
		Common.selectComboItem(kelamin, mahasiswa.getKelamin());
		row.appendChild(kelamin);
		kelamin.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tempat / Tanggal Lahir"));
		Box hbox = Common.isMobile() ? new Vbox() : new Hbox();
		hbox.appendChild(
				tempatlahir = new Textbox(mahasiswa.getTempatlahir() == null ? "" : mahasiswa.getTempatlahir()));
		hbox.appendChild(tanggallahir = new MyDatebox(
				mahasiswa.getTanggallahir() == null ? ais.ui.util.WaktuUtil.getDate() : mahasiswa.getTanggallahir()));
		row.appendChild(hbox);
		tempatlahir.setCols(15);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tinggi Badan / Berat Badan / Golongan darah"));
		berat_badan = new Decimalbox(
				new BigDecimal(mahasiswa == null || mahasiswa.getBerat_badan() == null
						? (biodataMahasiswa == null || biodataMahasiswa.getBeratBadan() == null ? 0
								: biodataMahasiswa.getBeratBadan())
						: mahasiswa.getBerat_badan()));
		berat_badan.setCols(3);
		tinggi_badan = new Decimalbox(
				new BigDecimal(mahasiswa == null || mahasiswa.getTinggi_badan() == null
						? (biodataMahasiswa == null || biodataMahasiswa.getTinggiBadan() == null ? 0
								: biodataMahasiswa.getTinggiBadan())
						: mahasiswa.getTinggi_badan()));
		tinggi_badan.setCols(3);
		golongan_darah = new Textbox(mahasiswa.getGolongan_darah() == null
				? (biodataMahasiswa == null ? "" : biodataMahasiswa.getGolonganDarah())
				: mahasiswa.getGolongan_darah());
		golongan_darah.setCols(3);
		row.appendChild(new Hbox(
				new Component[] { tinggi_badan, new Label("Cm / "), berat_badan, new Label("Kg / "), golongan_darah }));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Agama"));
		agama = new Combobox();
		Common.insertCombo(agama, "nama", Agama.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(agama, mahasiswa.getAgama());
		row.appendChild(agama);
		agama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Telp."));
		row.appendChild(telp = new Textbox(mahasiswa.getTelp() == null ? "" : mahasiswa.getTelp()));
		telp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Email"));
		row.appendChild(email = new Textbox(mahasiswa.getEmail() == null ? "" : mahasiswa.getEmail()));
		email.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. KTP"));
		row.appendChild(ktp = new Textbox(mahasiswa.getKtp()));
		ktp.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Alamat"));
		row.appendChild(alamat = new Textbox(
				mahasiswa.getAlamat() == null ? (biodataMahasiswa == null ? "" : biodataMahasiswa.getAlamat())
						: mahasiswa.getAlamat()));
		alamat.setWidth("90%");
		alamat.setRows(5);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenjang"));
		jenjang = new Combobox();
		Common.insertCombo(jenjang, "nama", Jenjang.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(jenjang, mahasiswa.getJenjang());
		row.appendChild(jenjang);
		jenjang.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mulai belajar di semester"));
		Common.selectComboItem(semesterMulai, mahasiswa.getSemesterMulai());
		row.appendChild(semesterMulai);
		semesterMulai.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal Mahasiswa"));
		statusAwalMahasiswa = new Combobox();
		Common.insertCombo(statusAwalMahasiswa, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(statusAwalMahasiswa, mahasiswa.getStatusAwalMahasiswa());
		row.appendChild(statusAwalMahasiswa);
		statusAwalMahasiswa.setWidth("90%");

		if (statusAwalMahasiswa.getSelectedItem() == null) {
			try {
				List<MyComboitemConfig> comboitems = statusAwalMahasiswa.getChildren();
				for (MyComboitemConfig comboitem1 : comboitems) {
					if (comboitem.getLabel().toLowerCase().contains("baru")) {
						statusAwalMahasiswa.setSelectedItem(comboitem1);
						break;
					}
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		return borderlayout;
	}

	class MahasiswaRegistrasiWisudaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PendaftaranWisuda pendaftaranWisuda = (PendaftaranWisuda) arg1;

			if (pendaftaranWisuda.getMahasiswa() == null) {
				arg0.detach();
				return;
			}

			Image image = new Image(
					CommonMedia.getUrlFotoPengguna(new Tbmuser(pendaftaranWisuda.getMahasiswa()), 152, 114));
			image.setWidth("100%");
			image.setParent(arg0);

			new Label(pendaftaranWisuda.getWisuda() == null ? "" : pendaftaranWisuda.getWisuda().getWisudaKe() + "")
					.setParent(arg0);

			RevisiHelper.createNewRevisi(PendaftaranWisuda.class, pendaftaranWisuda,
					pendaftaranWisuda.getMahasiswa().getNim()).setParent(arg0);

			new Label(pendaftaranWisuda.getMahasiswa() == null ? "" : pendaftaranWisuda.getMahasiswa().getNama())
					.setParent(arg0);

			new Label(
					pendaftaranWisuda.getPersetujuanWisuda() != null && pendaftaranWisuda.getPersetujuanWisuda() ? "Ya"
							: "Tidak")
					.setParent(arg0);

			new Label(pendaftaranWisuda.getStatusPersetujuanAdministrasiFakultas() != null
					&& pendaftaranWisuda.getStatusPersetujuanAdministrasiFakultas().equals(1) ? "Ya" : "Tidak")
					.setParent(arg0);

			new Label(pendaftaranWisuda.getStatusPersetujuanAdministrasi() != null
					&& pendaftaranWisuda.getStatusPersetujuanAdministrasi().equals(1) ? "Ya" : "Tidak").setParent(arg0);

			new Label(pendaftaranWisuda.getStatusPersetujuanKeuangan() != null
					&& pendaftaranWisuda.getStatusPersetujuanKeuangan().equals(1) ? "Ya" : "Tidak").setParent(arg0);
			new Label(pendaftaranWisuda.getStatusPersetujuanPerpustakaan() != null
					&& pendaftaranWisuda.getStatusPersetujuanPerpustakaan().equals(1) ? "Ya" : "Tidak").setParent(arg0);
			new Label(pendaftaranWisuda.getStatusPersetujuanPerpustakaanFakultas() != null
					&& pendaftaranWisuda.getStatusPersetujuanPerpustakaanFakultas().equals(1) ? "Ya" : "Tidak")
					.setParent(arg0);

			Hbox toolbar = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/edit-box-line.svg");
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pendaftaranWisuda);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
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

											Common.refreshDelete(pendaftaranWisuda);

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

			button = new MyToolbarbuttonConfig("", "/img/svg/printer.svg");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					DetailwisudaHelper.cetakBukti(pendaftaranWisuda);
				}
			});
			button.setParent(toolbar);
		}

	}

	private void init(PendaftaranWisuda pendaftaranWisuda) throws Exception {

		if (pendaftaranWisuda.getMahasiswa() == null) {
			return;
		}

		System.out.println("start");
		this.pendaftaranWisuda = pendaftaranWisuda;
		addWindow.setTitle(pendaftaranWisuda.getId() == null ? "Tambah Pendaftaran Wisuda" : "Ubah Pendaftaran Wisuda");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		West west = new West();
		west.setParent(borderlayout);
		west.setWidth("70%");
		ais.ui.util.ZkCompat.setFlex(west, true);

		mahasiswaPanel = initMain(pendaftaranWisuda.getMahasiswa());
		Common.freeze(mahasiswaPanel, true);
		west.appendChild(mahasiswaPanel);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No. Kursi"));
		row.appendChild(
				noKursi = new Textbox(pendaftaranWisuda.getNoKursi() == null ? "" : pendaftaranWisuda.getNoKursi()));
		noKursi.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("No Registrasi Wisuda"));
		row.appendChild(noRegistrasiWisuda = new Textbox(
				pendaftaranWisuda.getNoRegistrasiWisuda() == null ? "" : pendaftaranWisuda.getNoRegistrasiWisuda()));
		noRegistrasiWisuda.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Daftar Wisuda"));
		row.appendChild(
				new MyDatebox(pendaftaranWisuda.getTanggalDaftarWisuda() == null ? ais.ui.util.WaktuUtil.getDate()
						: pendaftaranWisuda.getTanggalDaftarWisuda()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan Keuangan"));
		row.appendChild(statusPersetujuanKeuangan = new MyCheckboxConfig());
		statusPersetujuanKeuangan.setChecked(pendaftaranWisuda.getStatusPersetujuanKeuangan() != null
				&& pendaftaranWisuda.getStatusPersetujuanKeuangan().equals(1));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan Administrasi"));
		row.appendChild(statusPersetujuanAdministrasi = new MyCheckboxConfig());
		statusPersetujuanAdministrasi.setChecked(pendaftaranWisuda.getStatusPersetujuanAdministrasi() != null
				&& pendaftaranWisuda.getStatusPersetujuanAdministrasi().equals(1));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan Perpustakaan"));
		row.appendChild(statusPersetujuanPerpustakaan = new MyCheckboxConfig());
		statusPersetujuanPerpustakaan.setChecked(pendaftaranWisuda.getStatusPersetujuanPerpustakaan() != null
				&& pendaftaranWisuda.getStatusPersetujuanPerpustakaan().equals(1));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan Perpustakaan " + "Fakultas"));
		row.appendChild(statusPersetujuanPerpustakaanFakultas = new MyCheckboxConfig());
		statusPersetujuanPerpustakaanFakultas
				.setChecked(pendaftaranWisuda.getStatusPersetujuanPerpustakaanFakultas() != null
						&& pendaftaranWisuda.getStatusPersetujuanPerpustakaanFakultas().equals(1));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan Administrasi " + "Fakultas"));
		row.appendChild(statusPersetujuanAdministrasiFakultas = new MyCheckboxConfig());
		statusPersetujuanAdministrasiFakultas
				.setChecked(pendaftaranWisuda.getStatusPersetujuanAdministrasiFakultas() != null
						&& pendaftaranWisuda.getStatusPersetujuanAdministrasiFakultas().equals(1));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Persetujuan Wisuda"));
		row.appendChild(persetujuanWisuda = new MyCheckboxConfig());
		persetujuanWisuda.setChecked(
				pendaftaranWisuda.getPersetujuanWisuda() != null && pendaftaranWisuda.getPersetujuanWisuda());

		persetujuanWisuda.addEventListener("onCheck", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (persetujuanWisuda.isChecked()) {

					if (!statusPersetujuanKeuangan.isChecked()) {
						persetujuanWisuda.setChecked(false);

						MyMessageboxConfig.show(
								"Persetujuan Wisuda bisa dilakukan apabila bagian keuangan sudah melakukan persetujuan",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}

					if (!statusPersetujuanAdministrasi.isChecked()) {
						persetujuanWisuda.setChecked(false);

						MyMessageboxConfig.show(
								"Persetujuan Wisuda bisa dilakukan apabila bagian administrasi sudah melakukan persetujuan",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}

					if (!statusPersetujuanPerpustakaan.isChecked()) {
						persetujuanWisuda.setChecked(false);

						MyMessageboxConfig.show(
								"Persetujuan Wisuda bisa dilakukan apabila bagian perpustakaan sudah melakukan persetujuan",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}

					if (!statusPersetujuanAdministrasiFakultas.isChecked()) {
						persetujuanWisuda.setChecked(false);

						MyMessageboxConfig.show(
								"Persetujuan Wisuda bisa dilakukan apabila bagian administrasi " + "Fakultas"
										+ " sudah melakukan persetujuan",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}

					if (!statusPersetujuanPerpustakaanFakultas.isChecked()) {
						persetujuanWisuda.setChecked(false);

						MyMessageboxConfig.show(
								"Persetujuan Wisuda bisa dilakukan apabila bagian perpustakaan " + "Fakultas"
										+ " sudah melakukan persetujuan",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return;
					}

					statusPersetujuanKeuangan.setDisabled(true);
					statusPersetujuanAdministrasi.setDisabled(true);
					statusPersetujuanPerpustakaan.setDisabled(true);
					statusPersetujuanAdministrasiFakultas.setDisabled(true);
					statusPersetujuanPerpustakaanFakultas.setDisabled(true);

				} else {
					statusPersetujuanKeuangan.setDisabled(false);
					statusPersetujuanAdministrasi.setDisabled(false);
					statusPersetujuanPerpustakaan.setDisabled(false);
					statusPersetujuanAdministrasiFakultas.setDisabled(false);
					statusPersetujuanPerpustakaanFakultas.setDisabled(false);

				}

			}
		});

		statusPersetujuanKeuangan.setDisabled(persetujuanWisuda.isChecked());
		statusPersetujuanAdministrasi.setDisabled(persetujuanWisuda.isChecked());
		statusPersetujuanPerpustakaan.setDisabled(persetujuanWisuda.isChecked());
		statusPersetujuanAdministrasiFakultas.setDisabled(persetujuanWisuda.isChecked());
		statusPersetujuanPerpustakaanFakultas.setDisabled(persetujuanWisuda.isChecked());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(
				pendaftaranWisuda.getKeterangan() == null ? "" : pendaftaranWisuda.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		System.out.println("end");
		MyHboxToolbar toolbar = new MyHboxToolbar();
		toolbar.setParent(south);
		MyToolbarbutton cancel = new MyToolbarbutton("fa-ban ", "Batal");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbutton save = new MyToolbarbutton("fa-graduation-cap", "Simpan / Daftar Wisuda");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);

		save = new MyToolbarbutton("fa-pencil-square-o", "Ubah Biodata");
		save.setVisible(edit);
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.freeze(mahasiswaPanel, false);
			}
		});
		save.setParent(toolbar);

		MyToolbarbutton cetak = new MyToolbarbutton("fa-print", "Bukti");
		cetak.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

				if (MahasiswaRegistrasiWisudaAction.this.pendaftaranWisuda.getId() == null) {
					MyMessageboxConfig.show("Klik \"Simpan / Daftar Wisuda\" untuk mencetak bukti pendaftaran",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return;
				}

				if (MahasiswaRegistrasiWisudaAction.this.pendaftaranWisuda.getNoKursi() == null
						|| !MahasiswaRegistrasiWisudaAction.this.pendaftaranWisuda.getNoKursi().isEmpty()) {
					String noKursi = MahasiswaRegistrasiWisudaAction.this.pendaftaranWisuda.getId().toString();

					while (noKursi.length() < 8) {
						noKursi = "0" + noKursi;
					}

					MahasiswaRegistrasiWisudaAction.this.pendaftaranWisuda.setNoKursi(noKursi);
					Common.refreshSaveOrUpdate(MahasiswaRegistrasiWisudaAction.this.pendaftaranWisuda);
				}

				@SuppressWarnings("rawtypes")
				Map parameters = ais.common.HashMapGenerator.getRand();
				parameters.put("id_mahasiswa",
						MahasiswaRegistrasiWisudaAction.this.pendaftaranWisuda.getMahasiswa().getId());
				parameters.put("id_pendaftaran_wisuda", MahasiswaRegistrasiWisudaAction.this.pendaftaranWisuda.getId());

				KrsMahasiswa krsMahasiswa = Common
						.singkronkanKrsMahasiswa(MahasiswaRegistrasiWisudaAction.this.pendaftaranWisuda.getMahasiswa());
				Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs");
				BiodataMahasiswa biodataMahasiswa = MahasiswaRegistrasiWisudaAction.this.pendaftaranWisuda
						.getMahasiswa().ambilBiodata();
				Judisium judisium = Common.hitungJudisium(
						MahasiswaRegistrasiWisudaAction.this.pendaftaranWisuda.getMahasiswa(), krsMahasiswa);
				parameters.put("judisium", judisium == null ? "" : judisium.getNama());
				parameters.put("judisium_en", judisium == null ? "" : judisium.getNamaen());
				Common.insertProperty(BiodataMahasiswa.class, biodataMahasiswa, parameters, "bio");

				Common.insertProperty(Mahasiswa.class,
						MahasiswaRegistrasiWisudaAction.this.pendaftaranWisuda.getMahasiswa(), parameters, "mhs");
				Common.insertProperty(Skripsi.class,
						MahasiswaRegistrasiWisudaAction.this.pendaftaranWisuda.getSkripsi(), parameters, "skripsi");

				Report.generatePDFReport(Report.PDF, parameters, "kartu_daftar_wisuda",
						ais.ui.util.WaktuUtil.getDate());
			}
		});
		cetak.setParent(toolbar);

		borderlayout.setParent(addWindow);

		System.out.println("end1");
	}

	public boolean onSave(Event event) throws Exception {

		PendaftaranWisudaDao pendaftaranWisudaDao = DaoFactory.getInstance().getPendaftaranWisudaDao();
		if (pendaftaranWisuda.getId() != null) {
			pendaftaranWisuda = pendaftaranWisudaDao.load(pendaftaranWisuda.getId());
		}
		// pendaftaranWisuda.setMahasiswa((Mahasiswa)
		// mahasiswa.getSelectedItem()
		// .getValue());
		pendaftaranWisuda.setKeterangan(keterangan.getValue());
		pendaftaranWisuda.setNoKursi(noKursi.getValue());
		pendaftaranWisuda.setNoRegistrasiWisuda(noRegistrasiWisuda.getValue());
		pendaftaranWisuda.setStatusPersetujuanKeuangan(statusPersetujuanKeuangan.isChecked() ? 1 : 0);
		pendaftaranWisuda.setStatusPersetujuanAdministrasi(statusPersetujuanAdministrasi.isChecked() ? 1 : 0);
		pendaftaranWisuda.setStatusPersetujuanPerpustakaan(statusPersetujuanPerpustakaan.isChecked() ? 1 : 0);
		pendaftaranWisuda
				.setStatusPersetujuanPerpustakaanFakultas(statusPersetujuanPerpustakaanFakultas.isChecked() ? 1 : 0);
		pendaftaranWisuda
				.setStatusPersetujuanAdministrasiFakultas(statusPersetujuanAdministrasiFakultas.isChecked() ? 1 : 0);

		pendaftaranWisuda.setPersetujuanWisuda(persetujuanWisuda.isChecked());

		if (pendaftaranWisuda.getId() != null) {
			pendaftaranWisudaDao.update(pendaftaranWisuda);
		} else {
			pendaftaranWisudaDao.save(pendaftaranWisuda);
		}

		if (!onSaveMahasiswa(event)) {
			return false;
		}

		if (eventListener != null) {
			eventListener.onEvent(new Event("", addWindow, MahasiswaRegistrasiWisudaAction.this.pendaftaranWisuda));
		}

		return true;
	}

	public boolean onSaveMahasiswa(Event event) throws Exception {
		if (nim.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data NIM Mahasiswa",
					"Kolom NIM Mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu NIM Mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (nama.getValue().trim().equals("")) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Nama",
					"Kolom Nama belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Nama.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (program.getSelectedItem() == null || program.getSelectedItem().getValue() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Program",
					"Kolom Program belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Program.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (kewarganegaraan.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Kewarganegaraan",
					"Kolom Kewarganegaraan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Kewarganegaraan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show(Common.getBahasaConfig("Jurusan") + " harus diisi", "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (jenjang.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenjang mahasiswa",
					"Kolom Jenjang mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenjang mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (semesterMulai.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Semester mulai belajar",
					"Kolom Semester mulai belajar belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Semester mulai belajar.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		MahasiswaDao mahasiswaDao = DaoFactory.getInstance().getMahasiswaDao();
		if (mahasiswa.getId() != null) {
			mahasiswa = mahasiswaDao.load(mahasiswa.getId());
		}

		mahasiswa.setKtp(ktp.getValue());
		mahasiswa.setWarganegara((String) kewarganegaraan.getSelectedItem().getValue());

		mahasiswa.setNegara((Negara) (negara.getAttribute("negara")));
		mahasiswa.setAgama((Agama) (agama.getSelectedItem() == null ? null : agama.getSelectedItem().getValue()));
		mahasiswa.setKeterangan(keterangan.getValue());

		mahasiswa.setNim(nim.getValue());
		mahasiswa.setAlamat(alamat.getValue());
		mahasiswa.setEmail(email.getValue());
		mahasiswa
				.setKelamin(kelamin.getSelectedItem() == null ? null : kelamin.getSelectedItem().getValue().toString());
		mahasiswa.setJurusan(
				(Jurusan) (jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue() == null ? null
						: jurusan.getSelectedItem().getValue()));
		mahasiswa.setNama(nama.getValue());
		mahasiswa.setTanggallahir(tanggallahir.getValue());
		mahasiswa.setTelp(telp.getValue());
		mahasiswa.setTempatlahir(tempatlahir.getValue());
		mahasiswa.setTahunangkatan(tahunangkatan.getValue() == null ? null : tahunangkatan.getValue().intValue());

		mahasiswa.setJenjang(
				(Jenjang) (jenjang.getSelectedItem() == null ? null : jenjang.getSelectedItem().getValue()));

		mahasiswa.setSemesterMulai(
				(String) (semesterMulai.getSelectedItem() == null ? null : semesterMulai.getSelectedItem().getValue()));

		mahasiswa.setStatusAwalMahasiswa((StatusAwalMahasiswa) (statusAwalMahasiswa.getSelectedItem() == null ? null
				: statusAwalMahasiswa.getSelectedItem().getValue()));

		mahasiswa.setBerat_badan(berat_badan.getValue() == null ? null : berat_badan.getValue().intValue());
		mahasiswa.setTinggi_badan(tinggi_badan.getValue() == null ? null : tinggi_badan.getValue().intValue());
		mahasiswa.setGolongan_darah(golongan_darah.getValue() == null ? null : golongan_darah.getValue().trim());
		mahasiswa.setProgram((String) program.getSelectedItem().getValue());

		if (mahasiswa.getId() != null) {
			if (mahasiswa.getPass() == null) {
				mahasiswa.setPass(mahasiswa.getNim());
			}
			mahasiswaDao.update(mahasiswa);
		} else {
			mahasiswa.setPass(Common.desEncrypter.get().encrypt(mahasiswa.getNim()));
			mahasiswaDao.save(mahasiswa);
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Session session = HibernateUtil.currentSession();
				biodataMahasiswa = (BiodataMahasiswa) session.createCriteria(BiodataMahasiswa.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa)).setMaxResults(1).uniqueResult();

				if (biodataMahasiswa != null) {
					biodataMahasiswa.setAlamat(alamat.getValue());
					biodataMahasiswa
							.setTinggiBadan(tinggi_badan.getValue() == null ? 0 : tinggi_badan.getValue().intValue());
					biodataMahasiswa
							.setBeratBadan(berat_badan.getValue() == null ? 0 : berat_badan.getValue().intValue());
					biodataMahasiswa.setGolonganDarah(golongan_darah.getValue());
					Common.refreshUpdate(session, (biodataMahasiswa));
				}

				if (dosen.getAttribute("myValue") != null) {
					mahasiswa.setDosen(((Dosen) dosen.getAttribute("myValue")).getId());

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa);
					krsMahasiswa.setDosenPa((Dosen) dosen.getAttribute("myValue"));
					krsMahasiswa.setKelas(mahasiswa.getKelas());
					Common.refreshSaveOrUpdate(krsMahasiswa);

					Integer tahapanTemp = krsMahasiswa.getTahapan();
					if (tahapanTemp != null && tahapanTemp == 0) {
						tahapanTemp = null;
					}

				}
			}
		});

		return true;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PendaftaranWisuda.class);
		criteria.createAlias("mahasiswa", "mahasiswa");
		criteria.add(searchwisuda.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.eq("wisuda", searchwisuda.getSelectedItem().getValue()))
				.add(searchPersetujuanWisuda.isChecked() ? Restrictions.eq("persetujuanWisuda", true)
						: Restrictions.sqlRestriction("1=1"))
				.add(searchPersetujuanAdministrasi.isChecked() ? Restrictions.eq("statusPersetujuanAdministrasi", 1)
						: Restrictions.sqlRestriction("1=1"))
				.add(searchPersetujuanAdministrasiFakultas.isChecked()
						? Restrictions.eq("statusPersetujuanAdministrasi" + "Fakultas", 1)
						: Restrictions.sqlRestriction("1=1"))
				.add(searchPersetujuanKeuangan.isChecked() ? Restrictions.eq("statusPersetujuanKeuangan", 1)
						: Restrictions.sqlRestriction("1=1"))
				.add(searchPersetujuanPerpustakaan.isChecked() ? Restrictions.eq("statusPersetujuanPerpustakaan", 1)
						: Restrictions.sqlRestriction("1=1"))
				.add(searchPersetujuanPerpustakaanFakultas.isChecked()
						? Restrictions.eq("statusPersetujuanPerpustakaan" + "Fakultas", 1)
						: Restrictions.sqlRestriction("1=1"))
				.add(Restrictions.ilike("mahasiswa.nama", searchmahasiswa.getValue(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("mahasiswa.nim", searchnim.getValue(), MatchMode.ANYWHERE));

		if (order)
			criteria.addOrder(Order.asc("mahasiswa.nim"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchwisuda == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<PendaftaranWisuda> pendaftaranWisuda = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();

		ListModel strset = new SimpleListModel(pendaftaranWisuda);
		grid.setRowRenderer(new MahasiswaRegistrasiWisudaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
