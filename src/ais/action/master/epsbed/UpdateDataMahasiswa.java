package ais.action.master.epsbed;


import ais.common.CommonSearchFilterHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Timer;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.temporary.IPKMahasiswa;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class UpdateDataMahasiswa extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Map<Long, Object[]> data = new HashMap<Long, Object[]>();
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private Progressmeter progressmeter;
	private Integer jmlmhsTerproses = 0;
	private int jumlahMahasiswa = 0;

	private Label proses;

	private Combobox searchfakultas;
	private Combobox searchjurusan;

	private MyToolbarbuttonConfig processButton;
	private Label myLabel = new Label();

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		tahunAkademik.setReadonly(true);
		jenisSemester.setReadonly(true);

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}
		});

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
			if (tbmuser.ambilJurusan() != null) {
				Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());
			}
		}
		processButton.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				new Thread(new Runnable() {

					@Override
					public void run() {
						new Thread(new Process()).start();
					}
				}).start();

				final Timer timer = new Timer(500);
				timer.setRepeats(true);

				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub

						if (jumlahMahasiswa == 0) {
							Clients.clearBusy();
							return;
						}
						int persen = (jmlmhsTerproses * 100) / jumlahMahasiswa;
						progressmeter.setValue(persen);
						proses.setValue("Memproses " + jmlmhsTerproses + " dari " + jumlahMahasiswa + " data (" + persen
								+ " %).. " + myLabel.getValue());

						Clients.showBusy(proses.getValue());

						processButton.setDisabled(!jmlmhsTerproses.equals(jumlahMahasiswa));
						System.out.println("total : " + jumlahMahasiswa + ", proses : " + jmlmhsTerproses
								+ ", persen = " + persen);

						if (jmlmhsTerproses.equals(jumlahMahasiswa)) {
							Clients.clearBusy();
							timer.stop();
							timer.detach();
						}
					}

				});
				timer.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				timer.start();
			}

		});

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setValue(Perkuliahan.GANJIL);
		comboitem.setLabel(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setValue(Perkuliahan.GENAP);
		comboitem.setLabel(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		Common.selectComboItem(jenisSemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);

		// progressmeter = new Progressmeter(0);
		progressmeter.setWidth("100%");

	}

	private class Process implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub

			doProcess();

		}

		@SuppressWarnings("unchecked")
		public void doProcess() {
			List<Long> mahasiswaIds = HibernateUtil.currentSession().createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.property("id"))

			.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null || searchjurusan.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
					: CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))
					.createAlias("jurusan", "jurusan")

			.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions.sqlRestriction("1=1")
					: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

			.list();

			jumlahMahasiswa = mahasiswaIds.size();
			jmlmhsTerproses = 0;

			String tahunStr = (String) tahunAkademik.getSelectedItem().getValue();

			String jenis = (String) jenisSemester.getSelectedItem().getValue();

			int tahun = Integer.parseInt(tahunStr.split("/")[0]);

			Session session = HibernateUtil.currentNativeSession();

			String sql = "delete from temporary.ipk_mahasiswa where tahun_akademik = '" + tahunStr
					+ "' and semester % 2 = " + (jenis.equals(Perkuliahan.GANJIL) ? "1" : "0") + ";";

			int result = session.createSQLQuery(sql).executeUpdate();
			System.out.println("delete ==> " + sql + ", result = " + result);

			for (Long mahasiswaId : mahasiswaIds) {
				Mahasiswa mahasiswa = (Mahasiswa) session.load(Mahasiswa.class, mahasiswaId);

				myLabel.setValue(mahasiswa.toString());

				Integer semester = Common.getSemester( mahasiswa.getTahunangkatan(),
						(String) jenisSemester.getSelectedItem().getValue(),
						mahasiswa.getPindahKeKampusIniMasukSemester(),tahun,
						mahasiswa.getSemesterMulai());

				KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(mahasiswa, semester, null, null);

				Double ipmhs = krsMahasiswa.getIps();
				Double ipkmhs = krsMahasiswa.getIpk();

				Integer sksmhss = krsMahasiswa.getSksYangDiambil();
				Integer sksmhs = krsMahasiswa.getSksk();

				Object[] objects = new Object[] { mahasiswa, ipkmhs, ipmhs, sksmhss, sksmhs };

				data.put(mahasiswa.getId(), objects);
				jmlmhsTerproses++;

				IPKMahasiswa ipkMahasiswa = new IPKMahasiswa();
				ipkMahasiswa.setMahasiswa(mahasiswa);
				ipkMahasiswa.setIp((Double) objects[2]);
				ipkMahasiswa.setIpk((Double) objects[1]);
				ipkMahasiswa.setSksCurrent((Integer) objects[4]);
				ipkMahasiswa.setSksTotal((Integer) objects[3]);
				ipkMahasiswa.setSemester(semester);
				ipkMahasiswa.setNama(mahasiswa.getNim() + "__" + mahasiswa.getNama());
				ipkMahasiswa.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());

				session.getTransaction().begin();
				session.save(ipkMahasiswa);
				session.getTransaction().commit();

				ipkMahasiswa = null;
				mahasiswa = null;
				objects = null;

			}
			HibernateUtil.closeSession();
		}
	}
}
