package ais.action.master.pmb.nim;

import java.util.List;

import ais.database.model.BiodataCalonMahasiswa;

public interface NimGenerator {

	public String generateNim(BiodataCalonMahasiswa calonMahasiswa);

	public String generateNim(BiodataCalonMahasiswa calonMahasiswa,
			List<String> nimPengecualian);

}
