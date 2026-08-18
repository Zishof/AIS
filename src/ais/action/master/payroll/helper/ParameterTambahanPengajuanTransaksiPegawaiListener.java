package ais.action.master.payroll.helper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ParameterTambahan;
import ais.database.model.file.LampiranLain;
import ais.database.model.payroll.KelompokParameterTambahanPengajuanTransaksiPegawai;
import ais.database.model.payroll.ParameterTambahanPengajuanTransaksiPegawai;
import ais.database.model.payroll.PengajuanTransaksiPegawai;
import ais.ui.util.MyLabelStyled;
import ais.ui.util.MyMessageboxConfig;

public class ParameterTambahanPengajuanTransaksiPegawaiListener implements EventListener {

	private List<Row> parameterRows;
	private Rows rows;
	private PengajuanTransaksiPegawai pengajuanPegawai;
	private Map<String, LampiranLain> lampiranLains;
	private Set<KelompokParameterTambahanPengajuanTransaksiPegawai> kelompokParameterTambahanPengajuanTransaksiPegawais;
	private boolean readonly = false;

	public ParameterTambahanPengajuanTransaksiPegawaiListener(PengajuanTransaksiPegawai pengajuanPegawai,
			Set<KelompokParameterTambahanPengajuanTransaksiPegawai> kelompokParameterTambahanPengajuanTransaksiPegawais,
			List<Row> parameterRows, Map<String, LampiranLain> lampiranLains, Rows rows, boolean readonly) {
		this.parameterRows = parameterRows;
		this.kelompokParameterTambahanPengajuanTransaksiPegawais = kelompokParameterTambahanPengajuanTransaksiPegawais;
		this.rows = rows;
		this.readonly = readonly;
		this.pengajuanPegawai = pengajuanPegawai;
		this.lampiranLains = lampiranLains;
	}

	public boolean validate() throws Exception {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return true;
		}
		for (Row row : parameterRows) {
			ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
			KelompokParameterTambahanPengajuanTransaksiPegawai kelompokParameterTambahanPengajuanTransaksiPegawai = (KelompokParameterTambahanPengajuanTransaksiPegawai) row
					.getAttribute("kelompokParameterTambahanPengajuanTransaksiPegawai");
			if (parameterTambahan != null && kelompokParameterTambahanPengajuanTransaksiPegawai != null) {
				String jenis = kelompokParameterTambahanPengajuanTransaksiPegawai.getId() + "->"
						+ parameterTambahan.getId();

				String val = ParameterTambahan.ambilVal(row, parameterTambahan);

				if (parameterTambahan.getWajibDiisi()
						&& (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null"))) {
					MyMessageboxConfig.show("Mohon maaf, pilihan \"" + parameterTambahan.getLabelInputan() + "\" belum dipilih. Langkah yang dapat dilakukan: (1) pilih nilai yang sesuai pada kolom tersebut; (2) pastikan pilihan tidak dikosongkan; (3) ulangi kembali proses ini. Jika masih mengalami kendala, hubungi Administrator.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					return false;
				}
				if (parameterTambahan.getLampiranWajibDiisi()) {
					if (parameterTambahan.getHarusMenyertakanLampiran() && !lampiranLains.keySet().contains(jenis)) {
						MyMessageboxConfig.show(
								"Untuk pilihan \"" + parameterTambahan.getLabelInputan()
										+ "\", lampiran harus di-upload",
								"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
						return false;
					}
				}
			}
		}
		return true;
	}

	public void onSave(PengajuanTransaksiPegawai pengajuanPegawai) {

		pengajuanPegawai.populateParameterTambahan(parameterRows);

	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	public void onEvent(Event event) throws Exception {

		for (Row row : parameterRows) {
			row.setVisible(false);
		}
		parameterRows.clear();

		EventListener isi = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				pengajuanPegawai.populateParameterTambahan(parameterRows);
			}
		};

		Session session = HibernateUtil.currentSession();

		for (KelompokParameterTambahanPengajuanTransaksiPegawai kelompokParameterTambahanPengajuanTransaksiPegawai : kelompokParameterTambahanPengajuanTransaksiPegawais) {

			MyFormRow rowParameterTambahan = new MyFormRow();
			rowParameterTambahan.setVisible(false);
			rowParameterTambahan.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowParameterTambahan, "2");
			rowParameterTambahan
					.appendChild(new MyLabelStyled(kelompokParameterTambahanPengajuanTransaksiPegawai.getNama() + ""));
			parameterRows.add(rowParameterTambahan);

			List<ParameterTambahan> parameterTambahans = ConstantValues
					.simpleList(
							session.createCriteria(ParameterTambahanPengajuanTransaksiPegawai.class)
									.add(Restrictions.eq("kelompokParameterTambahanPengajuanTransaksiPegawai",
											kelompokParameterTambahanPengajuanTransaksiPegawai))
									.createAlias("parameterTambahan", "parameterTambahan")
									.createAlias("kelompokParameterTambahanPengajuanTransaksiPegawai",
											"kelompokParameterTambahanPengajuanTransaksiPegawai")
									.add(Restrictions.eq("parameterTambahan.aktif", true))
									.add(Restrictions.eq("kelompokParameterTambahanPengajuanTransaksiPegawai.aktif",
											true))
									.setProjection(Projections.groupProperty("parameterTambahan.id")),
							ParameterTambahan.class, false);
			Collections.sort(parameterTambahans);

			boolean tampil = false;
			rowParameterTambahan.setVisible(!parameterTambahans.isEmpty());
			if (!parameterTambahans.isEmpty()) {

				for (ParameterTambahan parameterTambahan : parameterTambahans) {
					String jenis = kelompokParameterTambahanPengajuanTransaksiPegawai.getId() + "->"
							+ parameterTambahan.getId();

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					row.setValign("top");
					row.setAttribute("parameterTambahan", parameterTambahan);
					row.setValign("top");
					row.setAttribute("kelompokParameterTambahanPengajuanTransaksiPegawai",
							kelompokParameterTambahanPengajuanTransaksiPegawai);
					row.setParent(rows);
					row.appendChild(new Label(
							parameterTambahan.getLabelInputan() + (parameterTambahan.getWajibDiisi() ? " (*)" : " ")));
					if (!parameterTambahan.getKeterangan().trim().isEmpty()) {
						parameterRows.add(Common.initKeterangan(rows, parameterTambahan.getKeterangan().trim()));
					}
					String val = "";
					String ket = "";
					String[] spl = pengajuanPegawai.getParameterTambahanInds().split("\n");
					for (String d : spl) {
						String[] value = d.split("<=>");
						if (value[0].trim().equalsIgnoreCase(jenis)) {
							val = value.length > 1 ? value[1].trim() : "";
							try {
								ket = value.length > 0 ? value[value.length - 1] : "";
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/payroll/helper/ParameterTambahanPengajuanTransaksiPegawaiListener.java:163");

							}
						}
					}

					boolean t = ParameterTambahan.initComponent(row, rows, jenis, parameterRows, lampiranLains,
							pengajuanPegawai.getId(), val, ket, parameterTambahan, isi, readonly);

					// System.out.println("parameterTambahan -> " + parameterTambahan + " t " + t);

					tampil |= t;

				}
			}

			rowParameterTambahan.setVisible(tampil);
		}
	}
}
