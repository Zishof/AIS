package ais.action.master.library.barcode;

import java.util.List;

import ais.database.model.library.BatchItemPunyaBarcode;

public interface BarcodeGenerator {

	public String generateBarcode(BatchItemPunyaBarcode batchItemPunyaBarcode);

	public String generateBarcode(List<String> barcodePengecualian, BatchItemPunyaBarcode batchItemPunyaBarcode);

}
