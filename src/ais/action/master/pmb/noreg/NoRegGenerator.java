package ais.action.master.pmb.noreg;

import java.util.List;

import ais.database.model.BiodataCalonMahasiswa;

public interface NoRegGenerator {

	public String generateNoReg(BiodataCalonMahasiswa biodataCalonMahasiswa);

	public String generateNoReg(List<String> noRegPengecualian, BiodataCalonMahasiswa biodataCalonMahasiswa);

}
