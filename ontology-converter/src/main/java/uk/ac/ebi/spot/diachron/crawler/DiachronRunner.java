package uk.ac.ebi.spot.diachron.crawler;

import org.athena.imis.diachron.archive.datamapping.OntologyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.spot.diachron.utils.DiachronException;
import uk.ac.ebi.spot.diachron.utils.Utils;


import java.io.*;
import java.net.SocketException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

/**
 * Created by olgavrou on 06/11/2015.
 */
public class DiachronRunner {

    private Logger log = LoggerFactory.getLogger(getClass());

    private String newDatasetUri;
    private String storeChangesArguments;

    public DiachronRunner(String newDatasetUri, String storeChangesArguments) {
        this.newDatasetUri = newDatasetUri;
        this.storeChangesArguments = storeChangesArguments;
    }


    public void convertArchiveAndChangeDetection(String ontologyName, String fileLocation, String version, String oldRecordSetId, File outputDir, Collection<URI> filter, String integrationUrl, String archiveUrls, String changeDetectionUrl) {
        final OLSOntologyRetriever ret = new OLSOntologyRetriever();

        DiachronArchiverService archiveService = null;
        if (integrationUrl != null) {
            archiveService = new DiachronArchiverService(integrationUrl, archiveUrls, changeDetectionUrl);
        }

        OntologyConverter converter = new OntologyConverter();


        version = version.replace("releases/", "");
        version = version.replace("-",".");

        log.info("reading " + ontologyName + " " + version);

        InputStream stream = null;
        stream = ret.getOntology(fileLocation);
        if(stream == null){
            log.info("ERROR: Could not download ontology: " + ontologyName);
            return;
        }

        try {
            File original = new File(outputDir, ontologyName + "-" + version + ".owl");
            File output = new File(outputDir, ontologyName + "-diachronic-" + version + ".owl");
            if(!original.exists()) {
                FileOutputStream fos = new FileOutputStream(original);
                try {
                    int read = 0;
                    byte[] bytes = new byte[1024];

                    while ((read = stream.read(bytes)) != -1) {
                        fos.write(bytes, 0, read);
                    }
                } catch (SocketException e ){
                    log.info("ERROR: Could not read: " + ontologyName);
                    log.info(e.toString());
                    Utils utils = new Utils();
                    utils.writeInFile(this.storeChangesArguments + "/Report.txt", "FAIL: Could not read: " + ontologyName);
                    return;
                } finally {
                    if (fos != null){
                        fos.close();
                    }
                    if (stream != null){
                        stream.close();
                    }
                }

            }///else {
            //    if (stream != null){
            //        stream.close();
            //    }
            //}
            log.info("Finished writing " + ontologyName + " " + version);
            log.info("Starting to convert to diachron: " + ontologyName + " " + version);
         //   if(!output.exists()) {
                String reasoner;
                if (ontologyName.contains("EFO")){
                    reasoner = "hermit";
                } else {
                    reasoner = "elk";
                }
                FileInputStream inputStream = null;
                FileOutputStream outputStream = null;
                try {
                    inputStream = new FileInputStream(original);
                    outputStream = new FileOutputStream(output);
                    converter.convert(inputStream, outputStream, ontologyName, filter, reasoner);
                } catch (NullPointerException e ){
                    log.info("ERROR: Could not convert: " + ontologyName);
                    log.info(e.toString());
                    Utils utils = new Utils();
                    utils.writeInFile(this.storeChangesArguments + "/Report.txt", "FAIL: Could not convert: " + ontologyName);
                    return;
                } finally {
                    if (inputStream != null){
                        inputStream.close();
                    }
                    if (outputStream != null){
                        outputStream.close();
                    }
                }
           // }

            log.info("Finished converting to diachron:  " + ontologyName + " " + version);

            Utils utils = new Utils();
            if (archiveService != null) {
                String datasetId = archiveService.createDiachronicDatasetId(ontologyName, ontologyName, "EMBL-EBI");
                log.info("Archiving dataset " + ontologyName + " with archive id " + datasetId);
                String instanceId = archiveService.archive(output, datasetId, version);
                log.info("Archive successful, instance id = " + instanceId);
                String recordSetId = utils.getLatestDatasetsInfo(archiveUrls, datasetId,"recordSet", instanceId); //archiveService.getVersionId(instanceId); //
                log.info("Recordset id for version " + version + " = " + recordSetId);

                if (oldRecordSetId != null) {
                    log.info("Running change detection between " + recordSetId + " and " + oldRecordSetId);
                    try {
                        archiveService.runChangeDetection(recordSetId, oldRecordSetId, this.newDatasetUri);
                        //Store changes in a file for StoreChanges to use when called
                        DateFormat df = new SimpleDateFormat("yyyy.MM.dd");
                        Date date = Calendar.getInstance().getTime();
                        String dateString = df.format(date).toString();

                        FileOutputStream outputStr = new FileOutputStream(new File(this.storeChangesArguments + "/ChangesArguments.txt"), true) ;
                        String out = "-n " + ontologyName.toLowerCase() + " -cs " + this.newDatasetUri + " -ov " + oldRecordSetId + " -nv " + recordSetId + " -v " + version + " -d " + dateString;
                        outputStr.write(out.getBytes());
                        outputStr.write("\n".getBytes());
                        outputStr.close();
                        log.info("Wrote change arguments into ChangesArguments.txt file");
                        // change detection successful
                        utils.writeInFile(this.storeChangesArguments + "/Report.txt", "CHANGE DETECTION for ontology: " + ontologyName + " Old Version: " + oldRecordSetId + " New Version: " + recordSetId + " Version: " + version + " Date: " + dateString);
                    } catch (RuntimeException | DiachronException e){
                        log.info("Change Detection Fail");
                        utils.writeInFile(this.storeChangesArguments + "/Report.txt", "Change Detection FAIL: " + ontologyName);
                    }


                    //------------------ save changes to mongodb -----------------------
                    /*DateFormat df = new SimpleDateFormat("yyyy.MM.dd");
                    Date date = Calendar.getInstance().getTime();
                    String dateString = df.format(date).toString();
                    StoreChanges storeChanges = new StoreChanges(ontologyName.toLowerCase(),this.newDatasetUri,oldRecordSetId,recordSetId,version,dateString);
                    try {
                        String changes = storeChanges.getChanges();
                        if(changes != null){
                            storeChanges.storeChanges("diachron","change", changes);
                        } else {
                            log.info("No changes found for this ontology");
                        }
                    } catch (IOException e) {
                        log.info(e.toString());
                    } finally {
                        storeChanges.terminate();
                    }*/

                    //-------------------------------------------------------------------
                } else {
                    //oldRecordSetId is null, so, first archive
                    //if got to here, no error was thrown so archive was most probably successful
                    utils.writeInFile(this.storeChangesArguments + "/Report.txt", "FIRST ARCHIVE of ontology: " + ontologyName);
                }
            }
        } catch (IOException | DiachronException e) {
            log.info("ERROR: Could not convert and archive: " + ontologyName);
            log.info(e.toString());
            Utils utils = new Utils();
            utils.writeInFile(this.storeChangesArguments + "/Report.txt", "FAIL: Could not convert and archive: " + ontologyName);
            return;
        }
    }
}

