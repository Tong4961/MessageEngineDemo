package com.me.bp.process;

import com.me.bp.entity.ProcessConfig;
import com.me.bp.entity.ProcessNode;
import com.me.bp.service.HTTPService;
import com.me.common.RequestCommon;
import com.me.common.ResponseResult;
import org.apache.cxf.common.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * @ClassName ProcessEngine
 * @Description 流程引擎核心
 * @Author Ming
 * @Date 2026/4/22 14:19
 * @Version 1.0
 */
@Service
public class ProcessEngine {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Autowired
    private HTTPService httpService;

    public ResponseResult executeTest(String topic, RequestCommon requestCommon){
        ProcessContext processContext = new ProcessContext();
        processContext.setRequest(requestCommon);
        ProcessConfig processConfig = OBJECT_MAPPER.readValue(processConfigJson1, ProcessConfig.class);
        if ("sync".equals(requestCommon.getSyncType())) {
            /*try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }*/
        }
        String resp = "BP Result";
        ProcessNode currentProcessNode = getNodeById(processConfig, processConfig.getStartNodeId());
        while (currentProcessNode != null) {
            if (currentProcessNode == null || processConfig.getEndNodeId().equals(currentProcessNode.getNodeId())) {
                if (!StringUtils.isEmpty(processContext.getResponse())) {
                    resp =  processContext.getResponse();
                }
                break;
            }
            currentProcessNode = getNodeById(processConfig, currentProcessNode.getNextNodeId());
            if ("TEST".equals(currentProcessNode.getNodeType())) {
                System.out.println(OBJECT_MAPPER.writeValueAsString(currentProcessNode));
            }
            if ("HTTP".equals(currentProcessNode.getNodeType())) {
                httpService.doHTTPInvoking(processContext, currentProcessNode);
                System.out.println("doHTTPInvoking");
            }
            if ("SOAP".equals(currentProcessNode.getNodeType())) {
                System.out.println("doSOAPInvoking");
            }
        }
        ResponseResult reply = ResponseResult.success(requestCommon.getRequestId(),resp);
        return reply;
    }

    public ProcessNode getNodeById(ProcessConfig processConfig, String nodeId) {
        if (StringUtils.isEmpty(nodeId) || processConfig == null || CollectionUtils.isEmpty(processConfig.getProcessNodes()) ) {
            return null;
        }
        return processConfig.getProcessNodes().stream()
                .filter(node -> nodeId.equals(node.getNodeId()))
                .findFirst()
                .orElse(null);
    }

    private static String processConfigJson1= """
            {
              "processId": "JH0001",
              "processName": "JH0001流程",
              "processNodes": [
                {
                  "nodeId": "node1",
                  "nodeName": "开始节点",
                  "nodeType": "START",
                  "nodeConfig": "",
                  "nextNodeId": "node2"
                },
                {
                  "nodeId": "node2",
                  "nodeName": "node2",
                  "nodeType": "HTTP",
                  "nodeConfig": "{\\"boId\\":\\"2\\",\\"uri\\":\\"/api/create\\",\\"method\\":\\"POST\\",\\"contentType\\":\\"application/json\\",\\"paramLocation\\":\\"body\\",\\"headerExpression\\":\\"\\",\\"payloadExpression\\":\\"request.requestBody\\",\\"responseExpression\\":\\"context.resp1\\"}",
                  "nextNodeId": "node3"
                },
                {
                  "nodeId": "node3",
                  "nodeName": "node3",
                  "nodeType": "HTTP",
                  "nodeConfig": "{\\"boId\\":\\"2\\",\\"uri\\":\\"/api/update\\",\\"method\\":\\"POST\\",\\"contentType\\":\\"application/json\\",\\"paramLocation\\":\\"form\\",\\"headerExpression\\":\\"\\",\\"payloadExpression\\":\\"request.requestParams\\",\\"responseExpression\\":\\"context.resp1\\"}",
                  "nextNodeId": "node4"
                },
                {
                  "nodeId": "node4",
                  "nodeName": "node4",
                  "nodeType": "HTTP",
                  "nodeConfig": "{\\"boId\\":\\"2\\",\\"uri\\":\\"/api/query\\",\\"method\\":\\"GET\\",\\"contentType\\":\\"application/json\\",\\"paramLocation\\":\\"param\\",\\"headerExpression\\":\\"\\",\\"payloadExpression\\":\\"request.queryString\\",\\"responseExpression\\":\\"response\\"}",
                  "nextNodeId": "node5"
                },
                {
                  "nodeId": "node5",
                  "nodeName": "node5",
                  "nodeType": "SOAP",
                  "nodeConfig": "{\\"boId\\":\\"3\\",\\"method\\":\\"methodName\\",\\"headerExpression\\":\\"\\",\\"param\\":\\"[param1,param2]\\",\\"payloadExpression\\":\\"[context.payload1,context.payload2]\\"}",
                  "nextNodeId": "node6"
                },
                {
                  "nodeId": "node6",
                  "nodeName": "node6",
                  "nodeType": "TEST",
                  "nodeConfig": "",
                  "nextNodeId": "node7"
                },
                {
                  "nodeId": "node7",
                  "nodeName": "node7",
                  "nodeType": "TEST",
                  "nodeConfig": "",
                  "nextNodeId": "node8"
                },
                {
                  "nodeId": "node8",
                  "nodeName": "结束节点",
                  "nodeType": "END",
                  "nodeConfig": "",
                  "nextNodeId": ""
                }
              ],
              "startNodeId": "node1",
              "endNodeId": "node8"
            }
            """;
}
