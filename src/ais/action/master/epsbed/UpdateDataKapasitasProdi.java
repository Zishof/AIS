package ais.action.master.epsbed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyComboitemConfig;
import org.zkoss.zul.Label;
import ais.ui.util.MyMessageboxConfig;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Timer;
import ais.ui.util.MyToolbarbuttonConfig;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.epsbed.KapasitasMahasiswaBaru;

public class UpdateDataKapasitasProdi extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Map<Long, Object[]> data = new HashMap<Long, Object[]>();
	private Combobox tahunAkademik;
	private Combobox jenisSemester;
	private Progressmeter progressmeter;
	private Integer jmlProdiTerproses = 0;
	private int jumlahProdi = 0;

	private Label proses;
	private volatile Thread thread = new Thread(new Process());
	private Combobox searchfakultas;
	private Combobox searchjurusan;

	private MyToolbarbuttonConfig processButton;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page, org.zkoss.zk.ui.Component parent,org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {Common.doCheckSecurity();return super.doBeforeCompose(page, parent, compInfo);}public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);

		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" },
				Fakultas.class);
		searchfakultas.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				// TODO Auto-generated method stub

				Common.insertCombo(searchjurusan, new String[] { "nama",
						"kodeEpsbed" }, "jenjang", Jurusan.class, Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)), Restrictions
						.eq("fakultas", searchfakultas.getSelectedItem()
								.getValue()));
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

				thread = new Thread(new Process());
				if (!thread.isAlive()) {
					thread.start();
				}
				final Timer timer = new Timer(500);
				timer.setRepeats(true);

				timer.addEventListener("onTimer", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub

						if (jumlahProdi == 0) {
							return;
						}
						int persen = (jmlProdiTerproses * 100) / jumlahProdi;
						progressmeter.setValue(persen);
						proses.setValue("Memproses " + jmlProdiTerproses
								+ " dari " + jumlahProdi + " data (" + persen
								+ " %)");

						processButton.setDisabled(!jmlProdiTerproses
								.equals(jumlahProdi));
						// System.out.println("total : " + jumlahMahasiswa);
						// System.out.println("proses : " + jmlmhsTerproses);

						if (jmlProdiTerproses.equals(jumlahProdi)) {
							timer.stop();
							timer.detach();
						}
					}

				});
				timer.setParent(ExecutionsCtrl.getCurrentCtrl()
						.getCurrentPage().getFirstRoot());
				timer.start();
			}

		});

		// jenisSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setValue(Perkuliahan.GANJIL);
		comboitem.setLabel(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setValue(Perkuliahan.GENAP);
		comboitem.setLabel(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		Common.selectComboItem(jenisSemester,
				Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL
						: Perkuliahan.GENAP);

		// progressmeter = new Progressmeter(0);
		progressmeter.setWidth("100%");

	}

	private class Process implements Runnable {

		@Override
		public void run() {
			// TODO Auto-generated method stub

			try {
				doProcess();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e); 
			}

		}

		@SuppressWarnings("unchecked")
		public void doProcess() throws Exception {
			List<Jurusan> jurusans = HibernateUtil
					.currentSession()
					.createCriteria(Jurusan.class)
					.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null || searchfakultas.getSelectedItem().getValue()==null ? Restrictions
							.sqlRestriction("1=1") : Restrictions.eq(
							"fakultas", searchfakultas.getSelectedItem()
									.getValue()))

					.list();

			jumlahProdi = jurusans.size();
			jmlProdiTerproses = 0;
			if (tahunAkademik.getSelectedItem() == null) {
				MyMessageboxConfig.show("Tahun Akademik Harus Dipilih !");
				return;
			}
			String tahunStr = (String) tahunAkademik.getSelectedItem()
					.getValue();

			// int tahun = Integer.parseInt(tahunStr.split("/")[0]);
			Session session = HibernateUtil.currentNativeSession();
			for (Jurusan jurusan : jurusans) {
				System.out.println("Update Data Kapasitas Jurusan : "
						+ jurusan.getNama());
				Object[] objects = new Object[] {
						jurusan,
						CommonEpsbed.hitungJumlahPeminat(jurusan, tahunStr),
						CommonEpsbed.hitungJumlahLulus(jurusan, tahunStr),
						CommonEpsbed.hitungJumlahDaftarUlang(jurusan, tahunStr),
						CommonEpsbed.hitungJumlahMundur(jurusan, tahunStr) };
				data.put(jurusan.getId(), objects);
				jmlProdiTerproses++;
				KapasitasMahasiswaBaru kapasitasMahasiswaBaru = (KapasitasMahasiswaBaru) session
						.createCriteria(KapasitasMahasiswaBaru.class)
						.add(Restrictions.eq("jurusan", jurusan))
						.add(Restrictions.eq("tahunAkademik", tahunStr))
						.add(Restrictions.eq("ganjilGenap", jenisSemester
								.getSelectedItem().getValue()))
						.setMaxResults(1).uniqueResult();
				if (kapasitasMahasiswaBaru == null) {
					kapasitasMahasiswaBaru = new KapasitasMahasiswaBaru();

				}
				kapasitasMahasiswaBaru.setJurusan((Jurusan) objects[0]);
				kapasitasMahasiswaBaru.setTahunAkademik(tahunStr);
				kapasitasMahasiswaBaru.setGanjilGenap((String) jenisSemester
						.getSelectedItem().getValue());
				kapasitasMahasiswaBaru.setJumlahPendaftar((Integer) objects[1]);
				kapasitasMahasiswaBaru.setJumlahLulus((Integer) objects[2]);
				kapasitasMahasiswaBaru
						.setJumlahDaftarUlang((Integer) objects[3]);
				kapasitasMahasiswaBaru.setJumlahMundur((Integer) objects[4]);
				kapasitasMahasiswaBaru.setNama("__" + jurusan.getNama());
				session.getTransaction().begin();
				session.saveOrUpdate(kapasitasMahasiswaBaru);
				session.getTransaction().commit();

			}

			
			HibernateUtil.closeSession();
		}
	}
}
