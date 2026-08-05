package ais.action.master.sirs.chart.helper;

/**
 * Dulu subclass org.zkoss.zkex.zul.impl.JFreeChartEngine (PE-only) untuk
 * mewarnai dan memberi judul chart JFreeChart. Chart kini digambar oleh
 * ais.ui.util.MyChart (HTML/CSS, ZK CE) sehingga class ini cukup menjadi
 * pembawa judul; MyChart.setEngine() menerima objek ini sebagai no-op.
 */
public class KunjunganPasienRawatJalanMingguanChartHelper {

	private Integer tahun;
	private Integer bulan;

	public KunjunganPasienRawatJalanMingguanChartHelper(Integer tahun, Integer bulan) {
		this.tahun = tahun;
		this.bulan = bulan;
	}

	public Integer getTahun() {
		return tahun;
	}

	public Integer getBulan() {
		return bulan;
	}

	public String getJudul() {
		return "Kunjungan Pasien Rawat Jalan bulan " + bulan + " tahun " + tahun;
	}
}
