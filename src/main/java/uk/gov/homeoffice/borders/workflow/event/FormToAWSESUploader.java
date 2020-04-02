package uk.gov.homeoffice.borders.workflow.event;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;

import static java.lang.String.format;

@Slf4j
public class FormToAWSESUploader {

    private final RestHighLevelClient elasticsearchClient;
    private final RuntimeService runtimeService;


    public FormToAWSESUploader(RestHighLevelClient elasticsearchClient,
                               RuntimeService runtimeService1) {
        this.elasticsearchClient = elasticsearchClient;
        this.runtimeService = runtimeService1;
    }
    public void upload(String form,
                       String key,
                       HistoricProcessInstance processInstance,
                       String executionId) {


        IndexRequest indexRequest = new IndexRequest(processInstance.getBusinessKey()).id(key);
        indexRequest.source(form, XContentType.JSON);
        try {
            final IndexResponse index = elasticsearchClient.index(indexRequest, RequestOptions.DEFAULT);
            log.info("Document uploaded result response'{}'", index.getResult().getLowercase());
        } catch (IOException e) {
            runtimeService.createIncident(
                    FormVariableS3PersistListener.FAILED_TO_CREATE_ES_RECORD,
                    executionId,
                    format("Failed to create ES document for %s",
                            processInstance.getBusinessKey()),
                    e.getMessage()
            );
        }
    }
}
