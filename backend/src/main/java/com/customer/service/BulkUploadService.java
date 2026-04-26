package com.customer.service;

import com.customer.dto.BulkUploadResultDto;
import com.customer.entity.Customer;
import com.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStrings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/*
  Processes bulk customer uploads from .xlsx files using Apache POI Streaming API (SAX/XSSFReader).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BulkUploadService {

    private static final int BATCH_SIZE = 500;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CustomerRepository customerRepository;

    // We inject self-reference to call the transactional batch method from within the same service.
    // Spring proxy is needed; using @Autowired via field injection
    private BulkUploadService self;

    // Spring will inject via constructor
    @org.springframework.beans.factory.annotation.Autowired
    public void setSelf(BulkUploadService self) {
        this.self = self;
    }

    /*
     Parses the Excel file using SAX streaming and processes in batches.
     */
    public BulkUploadResultDto processFile(MultipartFile file) throws Exception {
        BulkUploadResultDto result = new BulkUploadResultDto();

        List<String[]> currentBatch = new ArrayList<>(BATCH_SIZE);
        // Row number tracking
        final int[] rowNum = {1};

        try (InputStream is = file.getInputStream();
             OPCPackage pkg = OPCPackage.open(is)) {

            XSSFReader reader = new XSSFReader(pkg);
            SharedStrings sst = reader.getSharedStringsTable();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();

            // Get the first sheet
            Iterator<InputStream> sheets = reader.getSheetsData();
            if (!sheets.hasNext()) {
                result.getErrors().add("No sheets found in the Excel file.");
                return result;
            }

            InputStream sheetStream = sheets.next();

            // Custom SAX handler that collects rows
            ExcelSheetHandler handler = new ExcelSheetHandler(sst) {
                @Override
                protected void onRow(String[] rowData) {
                    rowNum[0]++;
                    if (rowNum[0] == 2) {
                        // First data row (row 1 - header) is skipped
                    }
                    result.setTotalRows(result.getTotalRows() + 1);
                    currentBatch.add(rowData);

                    if (currentBatch.size() >= BATCH_SIZE) {
                        List<String[]> batchCopy = new ArrayList<>(currentBatch);
                        int batchStartRow = rowNum[0] - batchCopy.size() + 1;
                        self.processBatch(batchCopy, batchStartRow, result);
                        currentBatch.clear();
                    }
                }
            };

            parser.parse(new InputSource(sheetStream), handler);

            // Process remaining rows
            if (!currentBatch.isEmpty()) {
                int batchStartRow = rowNum[0] - currentBatch.size() + 1;
                self.processBatch(currentBatch, batchStartRow, result);
                currentBatch.clear();
            }

            sheetStream.close();
        }

        return result;
    }

    /*
      Processes one batch of rows.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processBatch(List<String[]> rows, int startRowNum, BulkUploadResultDto result) {
        // Collect all NICs in this batch to fetch existing customers in one query
        List<String> nics = rows.stream()
                .filter(r -> r.length >= 3 && r[2] != null && !r[2].trim().isEmpty())
                .map(r -> r[2].trim())
                .collect(Collectors.toList());

        Map<String, Customer> existingByNic = customerRepository.findByNicNumberIn(nics)
                .stream()
                .collect(Collectors.toMap(Customer::getNicNumber, c -> c));

        List<Customer> toInsert = new ArrayList<>();
        List<Customer> toUpdate = new ArrayList<>();
        int rowNum = startRowNum;

        for (String[] row : rows) {
            try {
                if (row.length < 3) {
                    result.setFailedCount(result.getFailedCount() + 1);
                    result.getErrors().add("Row " + rowNum + ": insufficient columns (expected 3: Name, DOB, NIC).");
                    rowNum++;
                    continue;
                }

                String name = row[0] != null ? row[0].trim() : "";
                String dobStr = row[1] != null ? row[1].trim() : "";
                String nic = row[2] != null ? row[2].trim() : "";

                if (name.isEmpty() || dobStr.isEmpty() || nic.isEmpty()) {
                    result.setFailedCount(result.getFailedCount() + 1);
                    result.getErrors().add("Row " + rowNum + ": Name, DOB, and NIC are mandatory.");
                    rowNum++;
                    continue;
                }

                LocalDate dob;
                try {
                    dob = LocalDate.parse(dobStr, DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    result.setFailedCount(result.getFailedCount() + 1);
                    result.getErrors().add("Row " + rowNum + ": Invalid date format '" + dobStr + "'. Expected yyyy-MM-dd.");
                    rowNum++;
                    continue;
                }

                if (existingByNic.containsKey(nic)) {
                    // UPDATE existing customer
                    Customer existing = existingByNic.get(nic);
                    existing.setName(name);
                    existing.setDateOfBirth(dob);
                    toUpdate.add(existing);
                } else {
                    // INSERT new customer
                    Customer newCustomer = new Customer();
                    newCustomer.setName(name);
                    newCustomer.setDateOfBirth(dob);
                    newCustomer.setNicNumber(nic);
                    toInsert.add(newCustomer);
                    // Add to map so duplicates within same batch are detected
                    existingByNic.put(nic, newCustomer);
                }

            } catch (Exception e) {
                result.setFailedCount(result.getFailedCount() + 1);
                result.getErrors().add("Row " + rowNum + ": Unexpected error – " + e.getMessage());
            }
            rowNum++;
        }

        // Batch save
        if (!toInsert.isEmpty()) {
            customerRepository.saveAll(toInsert);
            result.setSuccessCount(result.getSuccessCount() + toInsert.size());
        }
        if (!toUpdate.isEmpty()) {
            customerRepository.saveAll(toUpdate);
            result.setUpdatedCount(result.getUpdatedCount() + toUpdate.size());
        }
    }

    // Inner SAX Handler for XLSX sheet parsing

    /*
     Abstract SAX handler that converts XLSX cell data into String arrays per row.
     Subclasses override {@link #onRow(String[])} to receive each completed row.
     */
    private abstract static class ExcelSheetHandler extends DefaultHandler {

        private final SharedStrings sst;
        private boolean nextIsString;
        private String lastContent;
        private final List<String> currentRow = new ArrayList<>();
        private boolean inCell;
        private boolean isHeaderRow = true; // skip first row (header)

        ExcelSheetHandler(SharedStrings sst) {
            this.sst = sst;
        }

        protected abstract void onRow(String[] rowData);

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
            if ("c".equals(qName)) {
                // Determine cell type
                String cellType = attrs.getValue("t");
                nextIsString = "s".equals(cellType);
                inCell = true;
                lastContent = "";
            } else if ("row".equals(qName)) {
                currentRow.clear();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (inCell) {
                lastContent += new String(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ("v".equals(qName)) {
                String value = lastContent;
                if (nextIsString) {
                    int idx = Integer.parseInt(value);
                    value = sst.getItemAt(idx).getString();
                }
                currentRow.add(value);
                inCell = false;
            } else if ("row".equals(qName)) {
                if (isHeaderRow) {
                    isHeaderRow = false; // skip header
                } else {
                    onRow(currentRow.toArray(new String[0]));
                }
            }
        }
    }
}
