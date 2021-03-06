package uk.ac.ebi.spot.diachron.crawler;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.spot.diachron.utils.DiachronException;
import uk.ac.ebi.spot.diachron.utils.Utils;

import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Simon Jupp
 * @date 12/01/2015
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public class DiachronArchiverService {

    private String integrationService = "";
    private Logger log = LoggerFactory.getLogger(getClass());

//    private static String internalServerUrl = "http://banana.ebi.ac.uk:55000";
    private String archiver = "";
    private String changeDetecotr = "";
    public DiachronArchiverService(String integrationService, String archiver, String changeDetecotr) {

        this.integrationService = integrationService;
        this.archiver = archiver;
        this.changeDetecotr = changeDetecotr;
    }

    // returns an instance id
    public String createDiachronicDatasetId (String datasetName, String label, String creator) throws DiachronException {

        Utils utils = new Utils();
        String existingDatasetId = utils.getDiachronicDataset(this.archiver , datasetName); //getDiachronicDatasetId(datasetName);
        if (existingDatasetId == null) {
            HttpPost httpPost = new HttpPost(archiver + "/archive/dataset");
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            CreateArchiveRequest requestBean = new CreateArchiveRequest(datasetName, label, creator);
            JSONObject jsonRequestBean = new JSONObject(requestBean);
            System.out.println(jsonRequestBean.toString());


            try {
                StringEntity entity = new StringEntity(jsonRequestBean.toString());
                HttpClient client = new DefaultHttpClient();
                httpPost.setEntity(entity);
                HttpResponse response = client.execute(httpPost);
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
                }

                ObjectMapper mapper = new ObjectMapper();
                Response responseBean = mapper.readValue(response.getEntity().getContent(), Response.class);

                if (responseBean.isSuccess() && responseBean.getData() != null) {
                    return responseBean.getData();
                }
                else {
                    throw new DiachronException ("Couldn't create new dataset " + responseBean.getMessage());
                }

            } catch (IOException e) {
                throw new DiachronException ("Couldn't create new dataset, unable to handle input data", e);
            }

        }

        return existingDatasetId;
    }



    public String archive (File input, String diachronicDatasetId, String version) throws DiachronException {
        //TODO: GeneralUploadRDF
       /* String url = this.integrationService + "/webresources/GeneralUploadRDF/";
        HttpRequestHandler requestHandler = new HttpRequestHandler();
        try {
            HashMap map = new HashMap();
            map.put("file",input.getAbsolutePath());
            map.put("datasetName", "GO");
            map.put("label","GO");
            map.put("creator","EMBL-EBI");
            map.put("converterType","ontology");
            map.put("reasoner","elk");

            String archiveUrl = this.integrationService + "/webresources/GeneralUploadRDF/upload";
            HttpPost httpPost= new HttpPost(archiveUrl);

            FileBody uploadFilePart = new FileBody(input);
            StringBody datasetName = new StringBody("GO");
            StringBody label = new StringBody("GO");
            StringBody creator = new StringBody("EMBL-EBI");
            StringBody converterType = new StringBody("ontology");
            StringBody reasoner = new StringBody("elk");
            StringBody format = new StringBody("RDF/XML");
            StringBody filters = new StringBody("");



            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart("file", uploadFilePart);
            reqEntity.addPart("datasetName", datasetName);
            reqEntity.addPart("label", label);
            reqEntity.addPart("creator", creator);
            reqEntity.addPart("converterType", converterType);
            reqEntity.addPart("reasoner", reasoner);
            reqEntity.addPart("format", format);
            reqEntity.addPart("filters",filters);

            httpPost.setEntity(reqEntity);

            HttpClient client = new DefaultHttpClient();

            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }

            ObjectMapper mapper = new ObjectMapper();
            Response responseBean = mapper.readValue(response.getEntity().getContent(), Response.class);

            if (responseBean.isSuccess() && responseBean.getData() != null) {
                return responseBean.getData();
            }
            else {
                throw new DiachronException ("Couldn't archive dataset " + responseBean.getMessage());
            }

        } catch ( RuntimeException | IOException e) {
            log.info(e.toString());

        }
        throw new DiachronException ("Couldn't archive dataset: " + input.toString());
*/
        String archiveUrl = archiver + "/archive/dataset/version";
        HttpPost httpPost= new HttpPost(archiveUrl);

        log.debug("archiving" + diachronicDatasetId + " to " + archiveUrl);
        try {

            FileBody uploadFilePart = new FileBody(input);
            StringBody diachronicDatasetIdBody = new StringBody(diachronicDatasetId);
            StringBody versionBody = new StringBody(version);

            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart("DiachronicDatasetURI", diachronicDatasetIdBody);
            reqEntity.addPart("versionNumber", versionBody);
            reqEntity.addPart("DataFile", uploadFilePart);
            httpPost.setEntity(reqEntity);

            HttpClient client = new DefaultHttpClient();

            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }

            ObjectMapper mapper = new ObjectMapper();
            Response responseBean = mapper.readValue(response.getEntity().getContent(), Response.class);

            if (responseBean.isSuccess() && responseBean.getData() != null) {
                return responseBean.getData();
            }
            else {
                throw new DiachronException ("Couldn't archive dataset " + responseBean.getMessage());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new DiachronException ("Couldn't archive dataset: " + input.toString());

    }

    public void runChangeDetection (String newVersionId, String oldVersionId, String datasetUri) throws DiachronException {

        if (newVersionId != null && oldVersionId != null) {
           // HttpPost httpPost = new HttpPost(this.integrationService + "/webresources/ComplexChangeDispatcher");
            HttpPost httpPost = new HttpPost(this.changeDetecotr + "/diachron/change_detection");
            httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
//            ChangeRequest requestBean = new ChangeRequest(oldVersionId, newVersionId, true, new ArrayList<String>());

            String jsonRequestBean = "{\"Dataset_URI\":\"" + datasetUri + "\",\"Old_Version\":\"" + oldVersionId + "\",\"Ingest\":true,\"New_Version\":\"" + newVersionId + "\",\"Complex_Changes\":[\"Add Definition\",\"Add Synonym\",\"Delete Definition\",\"Delete Synonym\",\"ADD LABEL\",\"DELETE LABEL\",\"ADD CLASS\",\"DELETE CLASS\",\"Mark as Obsolete\"] ,\"Associations\": null}\n";
//            JSONObject jsonRequestBean = new JSONObject(requestBean);
            System.out.println(jsonRequestBean);


            try {
                StringEntity entity = new StringEntity(jsonRequestBean);
                HttpClient client = new DefaultHttpClient();
                httpPost.setEntity(entity);
                HttpResponse response = client.execute(httpPost);
                System.out.println(response.toString());
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
                }
                // do nothing with response as integration layer dispatcher services doesn't work properly
            } catch (IOException e) {
                throw new DiachronException ("Couldn't run change detection, unable to handle input data", e);
            }
        }
    }

    public List<String> getDiachronicDatasetInstantiationIds(String diachronicDatasetId) throws DiachronException {

        List<String> results = new ArrayList<String>();
        if (diachronicDatasetId != null) {
            try {
                HttpGet httpGet= new HttpGet(archiver + "/archive/templates?name=listDatasets&diachronicDatasetId=" + URLEncoder.encode(diachronicDatasetId, "UTF-8"));
                httpGet.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
                HttpClient client = new DefaultHttpClient();
                HttpResponse response = client.execute(httpGet);
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
                }

                String jsonResponse = getStringFromInputStream(response.getEntity().getContent());

                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(jsonResponse);
                if (!rootNode.get("data").isTextual()) {
                    Iterator<JsonNode> bindings = rootNode.get("data").get("results").get("bindings").getElements();
                    while (bindings.hasNext()) {
                        JsonNode element = bindings.next();
                        results.add(element.get("dataset").get("value").getTextValue());
                    }
                }

                return results;
            } catch (IOException e) {
                throw new DiachronException ("Couldn't create new dataset, unable to handle input data", e);
            }

        }
        return results;
    }

    // given a dataset instance id get the recordset id
    public String getVersionId (String instanceId) throws DiachronException {
        if (instanceId == null) {
            throw new DiachronException("Can't request null dataset id");
        }

        String getDatasetQuery = "PREFIX diachron:<http://www.diachron-fp7.eu/resource/> " +
                "SELECT ?record FROM <http://www.diachron-fp7.eu/archive/dictionary> " +
                "WHERE {?dataset a diachron:DiachronicDataset ; " +
                "diachron:hasInstantiation ?instance . ?instance diachron:hasRecordSet ?record . " +
                "VALUES ?instance { <" + instanceId + ">}}";
        // get all datasets and filter on name
        String jsonResponse = postArchiveSparqlQuery(getDatasetQuery);

        try {
            ObjectMapper mapper = new ObjectMapper();

            JsonNode rootNode = mapper.readTree(jsonResponse);
            Iterator<JsonNode> bindings = rootNode.get("data").get("results").get("bindings").getElements();
            while (bindings.hasNext()) {
                JsonNode element = bindings.next();
                return element.get("record").get("value").getTextValue();
            }
        } catch (IOException e) {
            throw new DiachronException ("Couldn't create new dataset, unable to handle input data", e);
        }

        throw new DiachronException ("No results for querying with that dataset");
    }

    public String getDiachronicDatasetId (String datasetName ) throws DiachronException {

        String getDatasetQuery = "PREFIX diachron:<http://www.diachron-fp7.eu/resource/> " +
                "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#> " +
                "SELECT ?dataset ?name FROM <http://www.diachron-fp7.eu/archive/dictionary> WHERE {?dataset a diachron:DiachronicDataset ; rdfs:label ?name}";
        // get all datasets and filter on name
        String jsonRepsonse = postArchiveSparqlQuery(getDatasetQuery);

        try {
            ObjectMapper mapper = new ObjectMapper();

            JsonNode rootNode = mapper.readTree(jsonRepsonse);
            Iterator<JsonNode> bindings = rootNode.get("data").get("results").get("bindings").getElements();
            while (bindings.hasNext()) {
                JsonNode element = bindings.next();
                String label = element.get("name").get("value").getTextValue();
                if (label.equals(datasetName)) {
                    return element.get("dataset").get("value").getTextValue();
                }
            }
        } catch (IOException e) {
            throw new DiachronException ("Couldn't create new dataset, unable to handle input data", e);
        }

        return null;
    }

    private String postArchiveSparqlQuery (String query) throws DiachronException {

        ArchiveQuery archiveQuery = new ArchiveQuery(query);
        HttpPost httpPost = new HttpPost(this.integrationService + "/webresources/archivingService/query");
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        JSONObject jsonRequestBean = new JSONObject(archiveQuery);

        try {
            StringEntity entity = new StringEntity(jsonRequestBean.toString());
            HttpClient client = new DefaultHttpClient();
            httpPost.setEntity(entity);
            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }

            return getStringFromInputStream(response.getEntity().getContent());
        } catch (ClientProtocolException e) {
            throw new DiachronException ("No results for querying with that dataset, client protocol exception: " + e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new DiachronException ("No results for querying with that dataset, unsupported encoding: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new DiachronException ("No results for querying with that dataset, IO exception: " + e.getMessage(), e);
        }

    }


    public static void main(String[] args) {
        DiachronArchiverService service = new DiachronArchiverService("http://localhost:8080/DACHRONIntegrationLayer", "http://localhost:8080/archive-web-services", "http://localhost:8080/ForthMaven");
        try {

            System.out.println("trying to create a new EFO...");
            String datasetId = service.createDiachronicDatasetId("EFO", "EFO", "Jupp");
            System.out.println("Created with id: " + datasetId);

            System.out.println("Diachronic dataset for EFO is :" + service.getDiachronicDatasetId("EFO"));

            String previousId = null;
            for (String instanceId : service.getDiachronicDatasetInstantiationIds(datasetId)) {

                System.out.println("Got and instance id : " + instanceId);

                String versionId = service.getVersionId(instanceId);
                if (versionId!=null) {
                    System.out.println("Record set id: " + versionId);

                    if (previousId != null) {
                        service.runChangeDetection(versionId, previousId, "Dataset_URI");

                    }
                    previousId = versionId;

                }
            }

        } catch (DiachronException e) {
            e.printStackTrace();
        }


    }


    // utility function to convert InputStream to String
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }


    // inner POJOs for Jackson JSON mapping
    public static class CreateArchiveRequest {
        public String datasetName;
        public String label;
        public String creator;

        public CreateArchiveRequest(String datasetName, String label, String creator) {
            this.datasetName = datasetName;
            this.label = label;
            this.creator = creator;
        }

        public String getDatasetName() {
            return datasetName;
        }

        public void setDatasetName(String datasetName) {
            this.datasetName = datasetName;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getCreator() {
            return creator;
        }

        public void setCreator(String creator) {
            this.creator = creator;
        }
    }

    public static class Response {
        public String message;
        public String data;
        public boolean success;

        public Response() {

        }

        public Response(String message, String data, boolean success) {
            this.message = message;
            this.data = data;
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    public static class ArchiveQuery {
        public String getQuery() {
            return query;
        }

        public void setQuery(String query) {
            this.query = query;
        }

        String query;
        public ArchiveQuery(String query) {
            this.query = query;
        }

    }

    private class DatasetAlreadyExistsException extends Throwable {
        public DatasetAlreadyExistsException(String s) {
            super(s);
        }
    }

    public static class ChangeRequest {

        @JsonProperty("Old_Version")
        public String V1;

        @JsonProperty("New_Version")
        public String V2;

        @JsonProperty("Ingest")
        public boolean ingest;

        @JsonProperty("Complex_Changes")
        public List<String> CCs;

        public ChangeRequest(String oldVersion, String newVersion,  boolean ingest, List<String> complexChanges) {
            this.V1 = oldVersion;
            this.V2 = newVersion;
            this.ingest = ingest;
            this.CCs = complexChanges;
        }


        public String getV1() {
            return V1;
        }

        public void setV1(String V1) {
            this.V1 = V1;
        }

        public String getV2() {
            return V2;
        }

        public void setV2(String V2) {
            this.V2 = V2;
        }

        public boolean isIngest() {
            return ingest;
        }

        public void setIngest(boolean ingest) {
            this.ingest = ingest;
        }

        public List<String> getCCs() {
            return CCs;
        }

        public void setCCs(List<String> CCs) {
            this.CCs = CCs;
        }
    }

}
