package com.me.bp.process;

import com.me.common.RequestCommon;
import com.me.common.ResponseResult;
import org.springframework.stereotype.Service;
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
        for (ProcessNode processNode : processConfig.getProcessNodes()) {
            System.out.println(OBJECT_MAPPER.writeValueAsString(processNode));
        }
        ResponseResult reply = ResponseResult.success(requestCommon.getRequestId(),"消费者同步返回内容");
        return reply;
    }

    private static String processConfigJson1= """
            {
              "processId": "JH0001",
              "processName": "JH0001流程",
              "processNodes": [
                {
                  "nodeId": "node1",
                  "nodeName": "node1",
                  "nodeType": "TEST",
                  "nodeConfig": ""
                },
                {
                  "nodeId": "node2",
                  "nodeName": "node2",
                  "nodeType": "TEST",
                  "nodeConfig": ""
                },
                {
                  "nodeId": "node2",
                  "nodeName": "node2",
                  "nodeType": "TEST",
                  "nodeConfig": ""
                }
              ]
            }
            """;
}
