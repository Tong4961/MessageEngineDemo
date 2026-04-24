package com.me.bp.process;

import com.me.bp.entity.ProcessConfig;
import com.me.bp.entity.ProcessNode;
import com.me.common.RequestCommon;
import com.me.common.ResponseResult;
import org.apache.cxf.common.util.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * @ClassName ProcessEngine
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/22 14:19
 * @Version 1.0
 */
@Service
public class ProcessEngine {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public ResponseResult executeTest(String topic, RequestCommon requestCommon){
        ProcessContext processContext = new ProcessContext();
        ProcessConfig processConfig = OBJECT_MAPPER.readValue(processConfigJson1, ProcessConfig.class);
        if ("sync".equals(requestCommon.getSyncType())) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        ProcessNode currentProcessNode = getNodeById(processConfig, processConfig.getStartNodeId());
        while (currentProcessNode != null) {
            if (currentProcessNode == null || processConfig.getEndNodeId().equals(currentProcessNode.getNodeId())) {
                break;
            }
            currentProcessNode = getNodeById(processConfig, currentProcessNode.getNextNodeId());
            if ("TEST".equals(currentProcessNode.getNodeType())) {
                System.out.println(OBJECT_MAPPER.writeValueAsString(currentProcessNode));
            }
            if ("HTTP".equals(currentProcessNode.getNodeType())) {
                System.out.println(OBJECT_MAPPER.writeValueAsString(currentProcessNode));
            }
            if ("SOAP".equals(currentProcessNode.getNodeType())) {
                System.out.println(OBJECT_MAPPER.writeValueAsString(currentProcessNode));
            }
        }
        ResponseResult reply = ResponseResult.success(requestCommon.getRequestId(),"消费者同步返回内容");
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
                  "nodeConfig": "{\\"boId\\":\\"bo1\\",\\"uri\\":\\"/api/create\\",\\"method\\":\\"POST\\",\\"contentType\\":\\"application/json\\",\\"invokeTypeType\\":\\"body\\",\\"header\\":\\"\\",\\"payload\\":\\"context.requestbody\\"}",
                  "nextNodeId": "node3"
                },
                {
                  "nodeId": "node3",
                  "nodeName": "node2",
                  "nodeType": "SOAP",
                  "nodeConfig": "{\\"boId\\":\\"bo2\\",\\"method\\":\\"methodName\\",\\"header\\":\\"\\",\\"param\\":\\"[param1,param2]\\",\\"payload\\":\\"[context.payload1,context.payload2]\\"}",
                  "nextNodeId": "node4"
                },
                {
                  "nodeId": "node4",
                  "nodeName": "node2",
                  "nodeType": "TEST",
                  "nodeConfig": "",
                  "nextNodeId": "node5"
                },
                {
                  "nodeId": "node5",
                  "nodeName": "node2",
                  "nodeType": "TEST",
                  "nodeConfig": "",
                  "nextNodeId": "node6"
                },
                {
                  "nodeId": "node6",
                  "nodeName": "结束节点",
                  "nodeType": "END",
                  "nodeConfig": "",
                  "nextNodeId": ""
                }
              ],
              "startNodeId": "node1",
              "endNodeId": "node6"
            }
            """;
}
