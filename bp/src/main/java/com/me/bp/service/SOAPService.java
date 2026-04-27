package com.me.bp.service;

import com.me.bp.entity.BO;
import com.me.bp.entity.HTTPConfig;
import com.me.bp.entity.ProcessNode;
import com.me.bp.mapper.BOMapper;
import com.me.bp.process.ProcessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * @ClassName HTTPUtil
 * @Description TODO
 * @Author Ming
 * @Date 2026/4/22 14:00
 * @Version 1.0
 */
@Service
public class SOAPService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Autowired
    private BOMapper boMapper;

    public void doSOAPInvoking(ProcessContext processContext, ProcessNode node){
        HTTPConfig config = OBJECT_MAPPER.readValue(node.getNodeConfig(), HTTPConfig.class);
        BO bo = boMapper.selectOneById(config.getBoId());
    }

}
